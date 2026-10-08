package ru.embtlab.smartprice.domain.usecase

import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import ru.embtlab.smartprice.domain.model.ParsedPriceTag
import ru.embtlab.smartprice.domain.model.PriceTagLayoutProfile
import ru.embtlab.smartprice.domain.model.ProductUnit
import ru.embtlab.smartprice.domain.util.FuzzyMatcher
import kotlin.math.hypot

class ParsePriceTagUseCase {

    private val junkTokens = listOf("артикул", "код", "партия", "годен", "годендо", "изготовлен", "ккал", "штрихкод")
    private val rubleTokens = listOf("руб", "рублей", "рубля", "руб.", "₽", "p", "р")

    private val gramTokens = listOf("г", "гр", "грамм", "g", "gr", "gm")
    private val kgTokens = listOf("кг", "кило", "кг.", "kg", "kilo")
    private val mlTokens = listOf("мл", "мл.", "ml", "m1")
    private val literTokens = listOf("л", "л.", "литр", "l")
    private val pieceTokens = listOf("шт", "шт.", "упак", "упаковка", "пак", "пачка", "pcs")

    private val inlinePriceRegex = Regex("""(\d{1,5})\s*[\.,■▪•·\-–—]\s*(\d{2})""")

    fun parseFromVisionText(
        visionText: Text,
        frameWidth: Int = 1000,
        frameHeight: Int = 500,
        learnedProfile: PriceTagLayoutProfile = PriceTagLayoutProfile()
    ): ParsedPriceTag {
        val allLines = visionText.textBlocks.flatMap { it.lines }
        val fullText = visionText.text

        // 1. Поиск веса / объема
        val (detectedQuantity, detectedUnit) = extractQuantityWithFuzzy(allLines, fullText)

        // 2. Центростремительный скоринг кандидатов цены
        val centerX = frameWidth / 2f
        val centerY = frameHeight / 2f
        val maxDist = hypot(centerX, centerY).coerceAtLeast(1f)

        val scoredCandidateLines = allLines.filter { line ->
            val text = line.text.trim()
            val words = text.split(Regex("""\s+"""))
            val hasDigits = text.any { it.isDigit() }
            val hasPercent = text.contains("%")
            val isJunk = words.any { word -> FuzzyMatcher.matchesAny(word, junkTokens) }

            hasDigits && !hasPercent && !isJunk
        }.map { line ->
            val box = line.boundingBox ?: Rect()
            val boxCenterX = box.centerX().toFloat()
            val boxCenterY = box.centerY().toFloat()
            val distToCenter = hypot(boxCenterX - centerX, boxCenterY - centerY)

            val centerWeight = (1.0f - (distToCenter / maxDist) * 0.85f).coerceIn(0.15f, 1.0f)
            val fontHeight = box.height().toFloat()
            val finalScore = fontHeight * centerWeight
            line to finalScore
        }.sortedByDescending { it.second }

        var detectedPrice: String? = null
        var chosenPriceBox: Rect? = null

        for ((rubleLine, _) in scoredCandidateLines) {
            val box = rubleLine.boundingBox ?: continue
            val rawLineText = rubleLine.text.trim()

            // Вариант 1: рубли и копейки в одной строке
            val inlineMatch = inlinePriceRegex.find(rawLineText)
            if (inlineMatch != null) {
                detectedPrice = "${inlineMatch.groupValues[1]}.${inlineMatch.groupValues[2]}"
                chosenPriceBox = box
                break
            }

            // Вариант 2: крупный блок рублей + геометрический захват копеек
            val rubleNumber = extractLeadingNumber(rawLineText)
            if (rubleNumber != null && rubleNumber != detectedQuantity && rubleNumber.toIntOrNull() in 5..99999) {
                val centsValue = findCentsGeometrically(
                    rubleBox = box,
                    allLines = allLines,
                    profile = learnedProfile
                )
                detectedPrice = if (centsValue != null) {
                    "$rubleNumber.$centsValue"
                } else {
                    rubleNumber
                }
                chosenPriceBox = box
                break
            }
        }

        if (detectedPrice == null) {
            detectedPrice = parseFallbackPriceFuzzy(allLines, detectedQuantity)
        }

        // 3. Универсальное геометрическое извлечение заголовка ценника без словарей
        val detectedTitle = extractVisualHeaderTitle(allLines, chosenPriceBox)

        return ParsedPriceTag(
            name = detectedTitle,
            price = detectedPrice,
            quantity = detectedQuantity,
            unit = detectedUnit
        )
    }

    /**
     * Универсальный геометрический поиск заголовка товара над блоком цены.
     */
    private fun extractVisualHeaderTitle(allLines: List<Text.Line>, priceBox: Rect?): String? {
        val candidateLines = allLines.filter { line ->
            val text = line.text.trim()
            val box = line.boundingBox ?: return@filter false

            // Строка должна находиться выше блока с ценой (с запасом 15px)
            val isAbovePrice = priceBox == null || box.bottom <= priceBox.top + 15
            // Должны присутствовать буквы (не чистый штрихкод или цена)
            val hasLetters = text.any { it.isLetter() }
            // Исключаем даты и длинные числовые коды
            val isNotDateOrBarcode = !text.contains(Regex("""\b\d{2}[./]\d{2}""")) && !text.contains(Regex("""\d{10,}"""))
            // Исключаем строки, содержащие только объем/вес (например: "0,95л" или "500 г")
            val isNotPureWeight = !text.matches(Regex("""^[\d.,\s]+[гГgGkKкКлЛlLмМmM][\w.]*$"""))
            // Исключаем строки со знаками валют или служебными маркерами цены
            val isNotPriceLabel = !text.contains("цена", ignoreCase = true) && !text.contains("руб", ignoreCase = true)

            isAbovePrice && hasLetters && isNotDateOrBarcode && !isNotPureWeight && isNotPriceLabel
        }

        // Выбираем строку с максимальной высотой шрифта в верхней части ценника
        val bestHeaderLine = candidateLines.maxByOrNull { it.boundingBox?.height() ?: 0 } ?: return null

        // Очищаем от служебных знаков и берем первые 2-4 смысловых слова
        val cleanWords = bestHeaderLine.text
            .replace(Regex("""[^a-zA-Zа-яА-ЯёЁ\s]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.length >= 2 }
            .take(4)

        if (cleanWords.isEmpty()) return null

        // Форматируем в опрятный вид с заглавными буквами (Title Case)
        return cleanWords.joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    private fun findCentsGeometrically(
        rubleBox: Rect,
        allLines: List<Text.Line>,
        profile: PriceTagLayoutProfile
    ): String? {
        val expectedRightMin = rubleBox.right - (rubleBox.width() * 0.25f)
        val expectedRightMax = rubleBox.right + (rubleBox.width() * profile.centsOffsetRatio * 1.6f)

        val minHeightRatio = (profile.centsHeightRatio * 0.55f).coerceAtLeast(0.12f)
        val maxHeightRatio = (profile.centsHeightRatio * 1.55f).coerceAtMost(0.90f)

        for (line in allLines) {
            val otherBox = line.boundingBox ?: continue
            if (otherBox == rubleBox) continue

            val isToTheRight = otherBox.left in expectedRightMin.toInt()..expectedRightMax.toInt()
            val isVerticallyAligned = otherBox.top >= (rubleBox.top - rubleBox.height() * 0.45f) &&
                    otherBox.top <= (rubleBox.bottom - rubleBox.height() * 0.10f)

            val actualHeightRatio = otherBox.height().toFloat() / rubleBox.height().coerceAtLeast(1)
            val isMatchingLearnedHeight = actualHeightRatio in minHeightRatio..maxHeightRatio

            if (isToTheRight && isVerticallyAligned && isMatchingLearnedHeight) {
                val digitsInLine = line.text.filter { it.isDigit() }
                if (digitsInLine.length >= 2) {
                    return digitsInLine.take(2)
                }
            }
        }
        return null
    }

    private fun extractQuantityWithFuzzy(allLines: List<Text.Line>, fullText: String): Pair<String?, ProductUnit?> {
        for (line in allLines) {
            val words = line.text.split(Regex("""[\s,/]+""")).filter { it.isNotBlank() }
            for (i in words.indices) {
                val word = words[i]
                val gluedMatch = Regex("""^(\d+(?:[.,]\d+)?)([a-zA-Zа-яА-Я²³]+)$""").find(word)
                if (gluedMatch != null) {
                    val numberPart = gluedMatch.groupValues[1].replace(',', '.')
                    val suffixPart = gluedMatch.groupValues[2]
                    val unit = resolveUnitFuzzy(suffixPart)
                    if (unit != null) return numberPart to unit
                }

                val numberOnly = word.replace(',', '.').toDoubleOrNull()
                if (numberOnly != null && i + 1 < words.size) {
                    val unit = resolveUnitFuzzy(words[i + 1])
                    if (unit != null) {
                        val formattedNum = if (word.contains('.')) word else word.replace(',', '.')
                        return formattedNum to unit
                    }
                }
            }
        }

        val gramRegex = Regex("""(\d{2,4})\s*[гГgGrR](?:р|Р)?(?!\w)""").find(fullText)?.groupValues?.get(1)
        if (gramRegex != null) return gramRegex to ProductUnit.GRAM

        val kgRegex = Regex("""(\d{1,2}(?:[.,]\d{1,3})?)\s*[кКkK][гГgG](?!\w)""").find(fullText)?.groupValues?.get(1)
        if (kgRegex != null) return kgRegex.replace(',', '.') to ProductUnit.KILOGRAM

        val mlRegex = Regex("""(\d{2,4})\s*[мМmM][лЛlL](?!\w)""").find(fullText)?.groupValues?.get(1)
        if (mlRegex != null) return mlRegex to ProductUnit.MILLILITER

        val lRegex = Regex("""(\d{1,2}(?:[.,]\d{1,3})?)\s*[лЛlL](?!\w)""").find(fullText)?.groupValues?.get(1)
        if (lRegex != null) return lRegex.replace(',', '.') to ProductUnit.LITER

        return null to null
    }

    private fun resolveUnitFuzzy(token: String): ProductUnit? {
        val clean = token.filter { it.isLetter() }
        if (clean.isBlank()) return null
        return when {
            FuzzyMatcher.matchesAny(clean, gramTokens, threshold = 0.65) -> ProductUnit.GRAM
            FuzzyMatcher.matchesAny(clean, kgTokens, threshold = 0.65) -> ProductUnit.KILOGRAM
            FuzzyMatcher.matchesAny(clean, mlTokens, threshold = 0.65) -> ProductUnit.MILLILITER
            FuzzyMatcher.matchesAny(clean, literTokens, threshold = 0.65) -> ProductUnit.LITER
            FuzzyMatcher.matchesAny(clean, pieceTokens, threshold = 0.65) -> ProductUnit.PIECE
            else -> null
        }
    }

    private fun extractLeadingNumber(text: String): String? {
        val clean = text.filter { it.isDigit() || it == '.' || it == ',' }
        val digitsOnly = clean.takeWhile { it.isDigit() }
        return if (digitsOnly.isNotEmpty()) digitsOnly else null
    }

    private fun parseFallbackPriceFuzzy(allLines: List<Text.Line>, detectedQuantity: String?): String? {
        for (line in allLines) {
            val words = line.text.split(Regex("""\s+"""))
            val hasRubleToken = words.any { FuzzyMatcher.matchesAny(it, rubleTokens, threshold = 0.70) }
            if (hasRubleToken) {
                val num = line.text.filter { it.isDigit() || it == '.' || it == ',' }
                if (num.isNotBlank() && num != detectedQuantity) {
                    return num.replace(',', '.')
                }
            }
        }
        return null
    }

    operator fun invoke(rawText: String): ParsedPriceTag {
        val (quantity, unit) = extractQuantityWithFuzzy(emptyList(), rawText)
        return ParsedPriceTag(price = null, quantity = quantity, unit = unit)
    }
}
package ru.embtlab.smartprice.domain.usecase

import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import ru.embtlab.smartprice.domain.model.ParsedPriceTag
import ru.embtlab.smartprice.domain.model.ProductConstants
import ru.embtlab.smartprice.domain.model.ProductUnit
import ru.embtlab.smartprice.domain.util.FuzzyMatcher
import kotlin.math.abs

class ParsePriceTagUseCase {

    // Словари для Fuzzy Matching
    private val junkTokens = listOf("артикул", "артикулы", "код", "партия", "годен", "годендо", "изготовлен", "ккал", "штрихкод")
    private val cardPriceTokens = listOf("карта", "карте", "картой", "х5", "клуб", "выгода", "акция")
    private val rubleTokens = listOf("руб", "рублей", "рубля", "руб.", "₽", "p", "р")

    private val gramTokens = listOf("г", "гр", "грамм", "граммов", "g", "gr", "gm")
    private val kgTokens = listOf("кг", "кило", "килограмм", "kg", "kilo")
    private val mlTokens = listOf("мл", "миллилитр", "ml", "m1")
    private val literTokens = listOf("л", "литр", "литра", "l")
    private val pieceTokens = listOf("шт", "штука", "штук", "упак", "упаковка", "пак", "пачка", "pcs")

    // Поиск явных цен в одной строке (включая квадратные точки и тире)
    private val inlinePriceRegex = Regex("""(\d{1,5})\s*[\.,■•·\-–]\s*(\d{2})""")

    /**
     * Полный умный парсинг с пространственным анализом и нечётким сравнением (Fuzzy Matching).
     */
    fun parseFromVisionText(visionText: Text): ParsedPriceTag {
        val allLines = visionText.textBlocks.flatMap { it.lines }
        val fullText = visionText.text

        // 1. Поиск названия товара по Fuzzy Matching из каталога ProductConstants
        val detectedName = matchNameFromCatalog(allLines)

        // 2. Поиск веса/количества и единицы измерения
        val (detectedQuantity, detectedUnit) = extractQuantityWithFuzzy(allLines, fullText)

        // 3. Поиск цены с отсечением мусора и привязкой копеек
        var detectedPrice: String? = null

        // Фильтруем строки для кандидатов главных рублей
        val candidateLines = allLines.filter { line ->
            val text = line.text.trim()
            val words = text.split(Regex("""\s+"""))
            val hasDigits = text.any { it.isDigit() }
            val hasPercent = text.contains("%")

            // Страховка Fuzzy: отсекаем строки, где написано "Артикул", "Годен до" или проценты скидок
            val isJunk = words.any { word -> FuzzyMatcher.matchesAny(word, junkTokens) }

            hasDigits && !hasPercent && !isJunk
        }.sortedByDescending { it.boundingBox?.height() ?: 0 }

        for (line in candidateLines) {
            val box = line.boundingBox ?: continue
            val rawLineText = line.text.trim()

            // Вариант А: рубли и копейки в одной строке
            val inlineMatch = inlinePriceRegex.find(rawLineText)
            if (inlineMatch != null) {
                detectedPrice = "${inlineMatch.groupValues[1]}.${inlineMatch.groupValues[2]}"
                break
            }

            // Вариант Б: крупное число рублей + надстрочные копейки рядом
            val rubleNumber = extractLeadingNumber(rawLineText)
            if (rubleNumber != null && rubleNumber != detectedQuantity && rubleNumber.toIntOrNull() in 5..99999) {
                val centsValue = findCentsNearby(rubleBox = box, allLines = allLines)
                detectedPrice = if (centsValue != null) {
                    "$rubleNumber.$centsValue"
                } else {
                    rubleNumber
                }
                break
            }
        }

        // Если цена не найдена геометрически, ищем с помощью Fuzzy по символам валют
        if (detectedPrice == null) {
            detectedPrice = parseFallbackPriceFuzzy(allLines, detectedQuantity)
        }

        return ParsedPriceTag(
            name = detectedName,
            price = detectedPrice,
            quantity = detectedQuantity,
            unit = detectedUnit
        )
    }

    /**
     * Поиск веса/количества с защитой через Fuzzy Matching от опечаток OCR (200r -> 200г, m1 -> мл).
     */
    private fun extractQuantityWithFuzzy(allLines: List<Text.Line>, fullText: String): Pair<String?, ProductUnit?> {
        for (line in allLines) {
            val words = line.text.split(Regex("""[\s,/]+""")).filter { it.isNotBlank() }

            for (i in words.indices) {
                val word = words[i]

                // Проверка 1: Число и единица слитно (например, "200г", "200Г", "200r", "0.5л", "1кг")
                val gluedMatch = Regex("""^(\d+(?:[.,]\d+)?)([a-zA-Zа-яА-Я²³]+)$""").find(word)
                if (gluedMatch != null) {
                    val numberPart = gluedMatch.groupValues[1].replace(',', '.')
                    val suffixPart = gluedMatch.groupValues[2]

                    val unit = resolveUnitFuzzy(suffixPart)
                    if (unit != null) {
                        return numberPart to unit
                    }
                }

                // Проверка 2: Число отдельно, единица в следующем слове (например, "200" "гр")
                val numberOnly = word.replace(',', '.').toDoubleOrNull()
                if (numberOnly != null && i + 1 < words.size) {
                    val nextWord = words[i + 1]
                    val unit = resolveUnitFuzzy(nextWord)
                    if (unit != null) {
                        val formattedNum = if (word.contains('.')) word else word.replace(',', '.')
                        return formattedNum to unit
                    }
                }
            }
        }

        // Резервный поиск по регуляркам, если слова были склеены нестандартно
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

    /**
     * Поиск названия товара из каталога ProductConstants с помощью нечеткого сопоставления.
     */
    private fun matchNameFromCatalog(allLines: List<Text.Line>): String? {
        for (line in allLines.take(4)) {
            val lineWords = line.text.split(Regex("""[\s,.:;!?-]+""")).filter { it.length >= 3 }
            for (word in lineWords) {
                val matchedPreset = ProductConstants.CATALOG_PRODUCT_PRESETS.firstOrNull { preset ->
                    val presetWords = preset.split(" ")
                    presetWords.any { pWord -> FuzzyMatcher.similarity(word, pWord) >= 0.78 }
                }
                if (matchedPreset != null) {
                    return matchedPreset
                }
            }
        }
        return null
    }

    private fun extractLeadingNumber(text: String): String? {
        val clean = text.filter { it.isDigit() || it == '.' || it == ',' }
        val digitsOnly = clean.takeWhile { it.isDigit() }
        return if (digitsOnly.isNotEmpty()) digitsOnly else null
    }

    private fun findCentsNearby(rubleBox: Rect, allLines: List<Text.Line>): String? {
        val maxSearchRight = rubleBox.right + (rubleBox.width() * 0.95f)
        val minSearchLeft = rubleBox.right - (rubleBox.width() * 0.30f)

        for (line in allLines) {
            val otherBox = line.boundingBox ?: continue

            val isToTheRight = otherBox.left in minSearchLeft.toInt()..maxSearchRight.toInt()
            val isVerticallyAligned = otherBox.top >= (rubleBox.top - rubleBox.height() * 0.35f) &&
                    otherBox.top <= (rubleBox.bottom - rubleBox.height() * 0.20f)

            if (isToTheRight && isVerticallyAligned) {
                val digitsInLine = line.text.filter { it.isDigit() }
                if (digitsInLine.length >= 2) {
                    return digitsInLine.take(2)
                }
            }
        }
        return null
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
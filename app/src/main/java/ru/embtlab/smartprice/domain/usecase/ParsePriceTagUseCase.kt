package ru.embtlab.smartprice.domain.usecase

import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import ru.embtlab.smartprice.domain.model.ParsedPriceTag
import ru.embtlab.smartprice.domain.model.ProductUnit
import ru.embtlab.smartprice.domain.util.FuzzyMatcher
import kotlin.math.hypot

class ParsePriceTagUseCase {

    private val junkTokens = listOf("код", "арт", "артикул", "партия", "годен", "изг", "штрихкод", "ккал", "скидка")
    private val rubleTokens = listOf("руб", "рублей", "руб.", "р", "p")

    private val gramTokens = listOf("г", "гр", "грамм", "g", "gr", "gm")
    private val kgTokens = listOf("кг", "кило", "кг.", "kg", "kilo")
    private val mlTokens = listOf("мл", "мл.", "ml", "m1")
    private val literTokens = listOf("л", "л.", "литр", "l")
    private val pieceTokens = listOf("шт", "шт.", "штука", "уп", "пак", "порц", "pcs")

    // Поиск цен с явными разделителями (■, •, -, точка, запятая)
    private val inlinePriceRegex = Regex("""(\d{1,5})\s*[\.,■▪•·\-–—]\s*(\d{2})""")

    fun parseFromVisionText(
        visionText: Text,
        frameWidth: Int = 1000,
        frameHeight: Int = 500
    ): ParsedPriceTag {
        val allLines = visionText.textBlocks.flatMap { it.lines }
        val rawFullText = visionText.text

        // 1. Нормализация OCR-опечаток перед поиском веса/количества (латинские r/g после цифр считаем граммами)
        val normalizedFullText = rawFullText
            .replace(Regex("""(?<=\d)\s*[rR]\b"""), "г")
            .replace(Regex("""(?<=\d)\s*[gG]\b"""), "г")

        // 2. Извлечение количества и единиц измерения
        val (detectedQuantity, detectedUnit) = extractQuantityWithFuzzy(allLines, normalizedFullText)

        // 3. Центростремительный скоринг: отбираем строки ближе к центру видоискателя
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

        for ((rubleLine, _) in scoredCandidateLines) {
            val rawLineText = rubleLine.text.trim()

            // Вариант А: рубли и копейки уже разделены знаками
            val inlineMatch = inlinePriceRegex.find(rawLineText)
            if (inlineMatch != null) {
                detectedPrice = "${inlineMatch.groupValues[1]}.${inlineMatch.groupValues[2]}"
                break
            }

            // Вариант Б: изолированное число рублей
            val rubleNumber = extractLeadingNumber(rawLineText)
            if (rubleNumber != null && rubleNumber != detectedQuantity && rubleNumber.toIntOrNull() in 5..99999) {
                detectedPrice = rubleNumber
                break
            }
        }

        if (detectedPrice == null) {
            detectedPrice = parseFallbackPriceFuzzy(allLines, detectedQuantity)
        }

        return ParsedPriceTag(
            price = detectedPrice,
            quantity = detectedQuantity,
            unit = detectedUnit
        )
    }

    private fun extractQuantityWithFuzzy(allLines: List<Text.Line>, fullText: String): Pair<String?, ProductUnit?> {
        for (line in allLines) {
            // Подменяем латинские артефакты в строках
            val normalizedLine = line.text
                .replace(Regex("""(?<=\d)\s*[rR]\b"""), "г")
                .replace(Regex("""(?<=\d)\s*[gG]\b"""), "г")

            val words = normalizedLine.split(Regex("""[\s,/]+""")).filter { it.isNotBlank() }
            for (i in words.indices) {
                val word = words[i]
                val gluedMatch = Regex("""^(\d+(?:[.,]\d+)?)([a-zA-Zа-яА-ЯёЁ]+)$""").find(word)
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

        // Поиск по регулярным выражениям с поддержкой русских и латинских букв
        val gramRegex = Regex("""(\d{2,4})\s*[гg][рr]?(?:амм)?(?!\w)""", RegexOption.IGNORE_CASE).find(fullText)?.groupValues?.get(1)
        if (gramRegex != null) return gramRegex to ProductUnit.GRAM

        val kgRegex = Regex("""(\d{1,2}(?:[.,]\d{1,3})?)\s*[кk][гg](?!\w)""", RegexOption.IGNORE_CASE).find(fullText)?.groupValues?.get(1)
        if (kgRegex != null) return kgRegex.replace(',', '.') to ProductUnit.KILOGRAM

        val mlRegex = Regex("""(\d{2,4})\s*[мm][лl](?!\w)""", RegexOption.IGNORE_CASE).find(fullText)?.groupValues?.get(1)
        if (mlRegex != null) return mlRegex to ProductUnit.MILLILITER

        val lRegex = Regex("""(\d{1,2}(?:[.,]\d{1,3})?)\s*[лl](?!\w)""", RegexOption.IGNORE_CASE).find(fullText)?.groupValues?.get(1)
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
        val normalized = rawText
            .replace(Regex("""(?<=\d)\s*[rR]\b"""), "г")
            .replace(Regex("""(?<=\d)\s*[gG]\b"""), "г")
        val (quantity, unit) = extractQuantityWithFuzzy(emptyList(), normalized)
        return ParsedPriceTag(price = null, quantity = quantity, unit = unit)
    }
}
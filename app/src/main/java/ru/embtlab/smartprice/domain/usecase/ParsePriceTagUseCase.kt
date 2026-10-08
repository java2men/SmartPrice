package ru.embtlab.smartprice.domain.usecase

import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import ru.embtlab.smartprice.domain.model.ParsedPriceTag
import ru.embtlab.smartprice.domain.model.ProductUnit
import kotlin.math.abs

class ParsePriceTagUseCase {

    private val weightGramRegex = Regex("""(\d{2,4})\s*[гГgG](?:р|Р)?(?!\w)""")
    private val weightKgRegex = Regex("""(\d{1,2}(?:[.,]\d{1,3})?)\s*[кКkK][гГgG](?!\w)""")
    private val volumeMlRegex = Regex("""(\d{2,4})\s*[мМmM][лЛlL](?!\w)""")
    private val volumeLiterRegex = Regex("""(\d{1,2}(?:[.,]\d{1,3})?)\s*[лЛlL](?!\w)""")
    private val pieceRegex = Regex("""(\d{1,3})\s*(?:шт|уп|пак)\b""", RegexOption.IGNORE_CASE)

    // Поиск цен с любыми разделителями: точка, запятая, квадратная точка (■, •, ·, -)
    private val inlinePriceRegex = Regex("""(\d{1,5})\s*[\.,■•·\-–]\s*(\d{2})""")
    private val currencyPriceRegex = Regex("""(\d{1,5}(?:[\.,]\d{1,2})?)\s*(?:₽|руб|р|p|P)\b""", RegexOption.IGNORE_CASE)
    private val dateOrBarcodeRegex = Regex("""\b(?:\d{2}[./]\d{2}[./]\d{2,4}|\d{10,13})\b""")

    /**
     * Интеллектуальный разбор с учетом разного шрифта, надстрочных копеек и квадратных точек.
     */
    fun parseFromVisionText(visionText: Text): ParsedPriceTag {
        val fullText = visionText.text
        val parsedQuantity = extractQuantity(fullText)

        val allLines = visionText.textBlocks.flatMap { it.lines }
        var detectedPrice: String? = null

        // 1. Фильтруем строки для кандидатов главных рублей
        val candidateRublesLines = allLines.filter { line ->
            val text = line.text.trim()
            !text.contains("%") &&
                    !dateOrBarcodeRegex.containsMatchIn(text) &&
                    text.any { it.isDigit() }
        }.sortedByDescending { it.boundingBox?.height() ?: 0 }

        for (rubleLine in candidateRublesLines) {
            val box = rubleLine.boundingBox ?: continue
            val rawLineText = rubleLine.text.trim()

            // Проверка А: рубли и копейки в одной строке через точку, тире или квадратную точку
            val inlineMatch = inlinePriceRegex.find(rawLineText)
            if (inlineMatch != null) {
                detectedPrice = "${inlineMatch.groupValues[1]}.${inlineMatch.groupValues[2]}"
                break
            }

            // Проверка Б: извлекаем число рублей из крупного блока
            val rubleNumber = extractLeadingNumber(rawLineText)
            if (rubleNumber != null && rubleNumber.toIntOrNull() in 5..99999) {
                // Ищем надстрочные копейки в соседних блоках справа от найденных рублей
                val centsValue = findCentsNearby(rubleBox = box, allLines = allLines)

                detectedPrice = if (centsValue != null) {
                    "$rubleNumber.$centsValue"
                } else {
                    rubleNumber
                }
                break
            }
        }

        // Если геометрический анализ не дал результата — запасной разбор
        if (detectedPrice == null) {
            detectedPrice = parseFallbackPrice(fullText, parsedQuantity.first)
        }

        return ParsedPriceTag(
            price = detectedPrice,
            quantity = parsedQuantity.first,
            unit = parsedQuantity.second
        )
    }

    private fun extractLeadingNumber(text: String): String? {
        val clean = text.filter { it.isDigit() || it == '.' || it == ',' }
        val digitsOnly = clean.takeWhile { it.isDigit() }
        return if (digitsOnly.isNotEmpty()) digitsOnly else null
    }

    /**
     * Геометрический поиск копеек справа от рублей:
     * захватывает надстрочный шрифт и очищает от значков валют (99P, 99р, 99 шт -> 99).
     */
    private fun findCentsNearby(rubleBox: Rect, allLines: List<Text.Line>): String? {
        val maxSearchRight = rubleBox.right + (rubleBox.width() * 0.95f)
        val minSearchLeft = rubleBox.right - (rubleBox.width() * 0.30f)

        for (line in allLines) {
            val otherBox = line.boundingBox ?: continue

            val isToTheRight = otherBox.left in minSearchLeft.toInt()..maxSearchRight.toInt()
            // Копейки обычно находятся в верхней половине высоты главных рублей
            val isVerticallyAligned = otherBox.top >= (rubleBox.top - rubleBox.height() * 0.35f) &&
                    otherBox.top <= (rubleBox.bottom - rubleBox.height() * 0.20f)

            if (isToTheRight && isVerticallyAligned) {
                // Извлекаем первые две цифры из блока, игнорируя символы валют (P, ₽, шт)
                val digitsInLine = line.text.filter { it.isDigit() }
                if (digitsInLine.length >= 2) {
                    return digitsInLine.take(2)
                }
            }
        }
        return null
    }

    operator fun invoke(rawText: String): ParsedPriceTag {
        val (quantity, unit) = extractQuantity(rawText)
        val price = parseFallbackPrice(rawText, quantity)
        return ParsedPriceTag(price = price, quantity = quantity, unit = unit)
    }

    private fun extractQuantity(text: String): Pair<String?, ProductUnit?> {
        val normalized = text
            .replace(Regex("""(?<=\d)\s*[rR]\b"""), "г")
            .replace(Regex("""(?<=\d)\s*[gG]\b"""), "г")
            .replace(dateOrBarcodeRegex, " ")

        val gram = weightGramRegex.find(normalized)?.groupValues?.get(1)
        if (gram != null) return gram to ProductUnit.GRAM

        val kg = weightKgRegex.find(normalized)?.groupValues?.get(1)?.replace(',', '.')
        if (kg != null) return kg to ProductUnit.KILOGRAM

        val ml = volumeMlRegex.find(normalized)?.groupValues?.get(1)
        if (ml != null) return ml to ProductUnit.MILLILITER

        val liter = volumeLiterRegex.find(normalized)?.groupValues?.get(1)?.replace(',', '.')
        if (liter != null) return liter to ProductUnit.LITER

        val pcs = pieceRegex.find(normalized)?.groupValues?.get(1)
        if (pcs != null) return pcs to ProductUnit.PIECE

        return null to null
    }

    private fun parseFallbackPrice(text: String, detectedQuantity: String?): String? {
        val clean = text.replace(dateOrBarcodeRegex, " ")

        val inlineMatch = inlinePriceRegex.find(clean)
        if (inlineMatch != null) {
            return "${inlineMatch.groupValues[1]}.${inlineMatch.groupValues[2]}"
        }

        val currencyMatch = currencyPriceRegex.find(clean)?.groupValues?.get(1)?.replace(',', '.')
        if (currencyMatch != null) return currencyMatch

        val numbers = Regex("""\b(\d{2,5})\b""").findAll(clean)
            .map { it.groupValues[1] }
            .filter { it != detectedQuantity && it.toIntOrNull() in 10..99999 }
            .toList()

        return numbers.maxByOrNull { it.toInt() }
    }
}
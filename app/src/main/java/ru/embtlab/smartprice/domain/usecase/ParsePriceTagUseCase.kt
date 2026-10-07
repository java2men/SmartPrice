package ru.embtlab.smartprice.domain.usecase

import com.google.mlkit.vision.text.Text
import ru.embtlab.smartprice.domain.model.ParsedPriceTag
import ru.embtlab.smartprice.domain.model.ProductUnit
import kotlin.math.abs

class ParsePriceTagUseCase {

    // Вес и объём (поддерживаем слитное написание "500г", "200Г", "1кг", "0.5л")
    private val weightGramRegex = Regex("""(\d{2,4})\s*[гГgG](?:р|Р)?(?!\w)""")
    private val weightKgRegex = Regex("""(\d{1,2}(?:[.,]\d{1,3})?)\s*[кКkK][гГgG](?!\w)""")
    private val volumeMlRegex = Regex("""(\d{2,4})\s*[мМmM][лЛlL](?!\w)""")
    private val volumeLiterRegex = Regex("""(\d{1,2}(?:[.,]\d{1,3})?)\s*[лЛlL](?!\w)""")
    private val pieceRegex = Regex("""(\d{1,3})\s*(?:шт|уп|пак)\b""", RegexOption.IGNORE_CASE)

    // Цены, записанные в одну строку (например, 149.99, 149,99, 229р, 49 руб)
    private val singleLinePriceRegex = Regex("""(\d{1,5})[.,](\d{2})""")
    private val currencyPriceRegex = Regex("""(\d{1,5}(?:[.,]\d{1,2})?)\s*(?:₽|руб|р)\b""", RegexOption.IGNORE_CASE)

    // Служебные шаблоны дат и штрихкодов для очистки
    private val dateOrBarcodeRegex = Regex("""\b(?:\d{2}[./]\d{2}[./]\d{2,4}|\d{10,13})\b""")

    /**
     * Основной метод разбора с использованием геометрии блоков ML Kit.
     * Находит самую крупную цену на ценнике и склеивает надстрочные копейки (229 + 99 = 229.99).
     */
    fun parseFromVisionText(visionText: Text): ParsedPriceTag {
        val fullText = visionText.text

        // 1. Поиск веса и объёма по всему распознанному тексту
        val parsedQuantity = extractQuantity(fullText)

        // 2. Поиск цены по визуальным блокам ML Kit
        val allLines = visionText.textBlocks.flatMap { it.lines }
        var detectedPrice: String? = null

        // Ищем строку с наибольшей высотой шрифта (главные рубли ценника — всегда самые крупные)
        val candidateRublesLines = allLines.filter { line ->
            val text = line.text.trim()
            // Исключаем даты, артикулы и проценты скидок (-15%)
            !text.contains("%") && !dateOrBarcodeRegex.containsMatchIn(text) &&
                    text.any { it.isDigit() }
        }.sortedByDescending { it.boundingBox?.height() ?: 0 }

        for (rubleLine in candidateRublesLines) {
            val rubleText = rubleLine.text.filter { it.isDigit() || it == '.' || it == ',' }
            val box = rubleLine.boundingBox ?: continue

            // Вариант А: рубли и копейки уже распознаны в одну строку (например "229.99")
            val inlineMatch = singleLinePriceRegex.find(rubleLine.text)
            if (inlineMatch != null) {
                detectedPrice = "${inlineMatch.groupValues[1]}.${inlineMatch.groupValues[2]}"
                break
            }

            // Вариант Б: копейки напечатаны мелким верхним индексом справа от рублей
            val rubleDigits = rubleText.takeWhile { it.isDigit() }
            if (rubleDigits.isNotEmpty() && rubleDigits.toIntOrNull() in 5..99999) {
                // Ищем соседний блок копеек справа от рублей
                val centsLine = allLines.find { otherLine ->
                    val otherBox = otherLine.boundingBox ?: return@find false
                    val otherText = otherLine.text.filter { it.isDigit() }

                    val isRightOfRubles = otherBox.left >= (box.right - box.width() * 0.25f) &&
                            otherBox.left <= (box.right + box.width() * 0.8f)
                    val isVerticallyAligned = abs(otherBox.top - box.top) <= box.height() * 0.7f
                    val hasTwoDigits = otherText.length == 2

                    isRightOfRubles && isVerticallyAligned && hasTwoDigits
                }

                if (centsLine != null) {
                    val centsDigits = centsLine.text.filter { it.isDigit() }.take(2)
                    detectedPrice = "$rubleDigits.$centsDigits"
                } else {
                    detectedPrice = rubleDigits
                }
                break
            }
        }

        // Если геометрический анализ не дал результата — запасной Regex-парсинг
        if (detectedPrice == null) {
            detectedPrice = parseFallbackPrice(fullText, parsedQuantity.first)
        }

        return ParsedPriceTag(
            price = detectedPrice,
            quantity = parsedQuantity.first,
            unit = parsedQuantity.second
        )
    }

    /**
     * Запасной разбор по сырой строке
     */
    operator fun invoke(rawText: String): ParsedPriceTag {
        val (quantity, unit) = extractQuantity(rawText)
        val price = parseFallbackPrice(rawText, quantity)
        return ParsedPriceTag(price = price, quantity = quantity, unit = unit)
    }

    private fun extractQuantity(text: String): Pair<String?, ProductUnit?> {
        val sanitized = text.replace(dateOrBarcodeRegex, " ")

        val gram = weightGramRegex.find(sanitized)?.groupValues?.get(1)
        if (gram != null) return gram to ProductUnit.GRAM

        val kg = weightKgRegex.find(sanitized)?.groupValues?.get(1)?.replace(',', '.')
        if (kg != null) return kg to ProductUnit.KILOGRAM

        val ml = volumeMlRegex.find(sanitized)?.groupValues?.get(1)
        if (ml != null) return ml to ProductUnit.MILLILITER

        val liter = volumeLiterRegex.find(sanitized)?.groupValues?.get(1)?.replace(',', '.')
        if (liter != null) return liter to ProductUnit.LITER

        val pcs = pieceRegex.find(sanitized)?.groupValues?.get(1)
        if (pcs != null) return pcs to ProductUnit.PIECE

        return null to null
    }

    private fun parseFallbackPrice(text: String, detectedQuantity: String?): String? {
        val clean = text.replace(dateOrBarcodeRegex, " ")

        // 1. Поиск цены с копейками
        val centsMatch = singleLinePriceRegex.find(clean)
        if (centsMatch != null) {
            return "${centsMatch.groupValues[1]}.${centsMatch.groupValues[2]}"
        }

        // 2. Поиск числа со знаком рубля (229р, 49 ₽)
        val currencyMatch = currencyPriceRegex.find(clean)?.groupValues?.get(1)?.replace(',', '.')
        if (currencyMatch != null) return currencyMatch

        // 3. Любое целое число от 10 до 99999, не совпадающее с граммами
        val numbers = Regex("""\b(\d{2,5})\b""").findAll(clean)
            .map { it.groupValues[1] }
            .filter { it != detectedQuantity && it.toIntOrNull() in 10..99999 }
            .toList()

        return numbers.maxByOrNull { it.toInt() }
    }
}
package ru.embtlab.smartprice.domain.usecase.voice

import ru.embtlab.smartprice.domain.model.product.DiscountType
import ru.embtlab.smartprice.domain.model.ocr.ParsedVoiceInput
import ru.embtlab.smartprice.domain.model.product.ProductUnit
import ru.embtlab.smartprice.domain.util.FuzzyMatcher
import ru.embtlab.smartprice.domain.util.RussianNumberWordParser

class ParseVoiceInputUseCase {

    private val unitsMap = listOf(
        listOf("г", "гр", "грамм", "грамма", "граммов", "g", "gr") to ProductUnit.GRAM,
        listOf("кг", "кило", "килограмм", "килограмма", "килограммов", "kg") to ProductUnit.KILOGRAM,
        listOf("мл", "миллилитр", "миллилитра", "миллилитров", "ml") to ProductUnit.MILLILITER,
        listOf("л", "литр", "литра", "литров", "l") to ProductUnit.LITER,
        listOf("шт", "штука", "штуки", "штук", "упаковка", "пачка") to ProductUnit.PIECE
    )

    operator fun invoke(rawSpokenText: String): ParsedVoiceInput {
        if (rawSpokenText.isBlank()) return ParsedVoiceInput()

        var text = rawSpokenText.lowercase().trim().replace(',', '.')

        // 1. Разговорные меры
        text = text.replace(Regex("""\bполтора\b"""), "1.5")
            .replace(Regex("""\bполкило\b"""), "500 грамм")
            .replace(Regex("""\bпол-литра\b"""), "0.5 литр")
            .replace(Regex("""\bполлитра\b"""), "0.5 литр")
            .replace(Regex("""\bс половиной\b"""), ".5")
            .replace(Regex("""\bи половина\b"""), ".5")

        // 2. Преобразование текстовых числительных в цифры
        text = RussianNumberWordParser.parseTextToDigits(text)

        // 3. Аккуратная обработка копеек:
        // А) Явные копейки: "50 рублей 50 копеек" или "50 руб 20 коп" -> 50.20
        val explicitKopeckRegex = Regex("""(\d+)\s*(?:рубл\w*|руб\.?|₽)\s*(\d{1,2})\s*(?:коп\w*)""")
        text = explicitKopeckRegex.replace(text) { match ->
            val rub = match.groupValues[1]
            val kop = match.groupValues[2].padStart(2, '0').take(2)
            "$rub.$kop рублей "
        }

        // Б) Только копейки: "50 копеек" -> 0.50 рублей
        val onlyKopeckRegex = Regex("""\b(\d{1,2})\s*коп\w*""")
        text = onlyKopeckRegex.replace(text) { match ->
            val kop = match.groupValues[1].padStart(2, '0').take(2)
            "0.$kop рублей "
        }

        // В) Дробные связки со словами "и / точка / целых" (например "50 и 50", но НЕ "50 рублей 1 литр"):
        val numberAndFractionRegex = Regex("""(\d+)\s+(?:точка|целых|целая)\s+(\d{1,2})\b""")
        text = numberAndFractionRegex.replace(text) { match ->
            val whole = match.groupValues[1]
            val frac = match.groupValues[2]
            "$whole.$frac "
        }

        // 4. Поиск акций и скидок
        var detectedDiscountType: DiscountType? = null
        var detectedDiscountPercent: String? = null

        val isOnePlusOne = Regex("""\b(1\+1|1 \+ 1|1 плюс 1|один плюс один|два по цене одного|второй бесплатно|вторая бесплатно)\b""").containsMatchIn(text)
        val isTwoPlusOne = Regex("""\b(2\+1|2 \+ 1|2 плюс 1|два плюс один|три по цене двух|третий бесплатно|третья бесплатно)\b""").containsMatchIn(text)

        when {
            isOnePlusOne -> {
                detectedDiscountType = DiscountType.ONE_PLUS_ONE
                text = text.replace(Regex("""\b(1\+1|1 \+ 1|1 плюс 1|один плюс один|два по цене одного|второй бесплатно|вторая бесплатно)\b"""), " ")
            }
            isTwoPlusOne -> {
                detectedDiscountType = DiscountType.TWO_PLUS_ONE
                text = text.replace(Regex("""\b(2\+1|2 \+ 1|2 плюс 1|два плюс один|три по цене двух|третий бесплатно|третья бесплатно)\b"""), " ")
            }
            else -> {
                val percentRegex = Regex("""(?:скидк\w*|минус|-)\s*(\d{1,2}(?:\.\d{1,2})?)\s*(?:%|процент\w*)?""")
                val percentMatch = percentRegex.find(text)
                if (percentMatch != null) {
                    detectedDiscountType = DiscountType.PERCENT
                    detectedDiscountPercent = percentMatch.groupValues[1]
                    text = text.replace(percentMatch.value, " ")
                } else if (text.contains("%") || text.contains("процент")) {
                    val standalonePercent = Regex("""(\d{1,2}(?:\.\d{1,2})?)\s*(?:%|процент\w*)""").find(text)
                    if (standalonePercent != null) {
                        detectedDiscountType = DiscountType.PERCENT
                        detectedDiscountPercent = standalonePercent.groupValues[1]
                        text = text.replace(standalonePercent.value, " ")
                    }
                }
            }
        }

        var detectedQuantity: String? = null
        var detectedUnit: ProductUnit? = null
        var detectedPrice: String? = null

        // 5. ЯКОРЬ КОЛИЧЕСТВА: сначала ищем число, стоящее вплотную к единице измерения (например "1 литр", "900 г", "2 шт")
        for ((tokens, unit) in unitsMap) {
            val tokensPattern = tokens.joinToString("|") { Regex.escape(it) }
            val unitRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:$tokensPattern)\b|(?:\b(?:$tokensPattern)\s*(\d+(?:\.\d+)?))""")
            val match = unitRegex.find(text)
            if (match != null) {
                detectedQuantity = match.groupValues[1].ifEmpty { match.groupValues[2] }
                detectedUnit = unit
                text = text.replaceRange(match.range, " ")
                break
            }
        }

        // Если единица измерения названа без числа (например, просто "литр") -> считаем количество 1
        if (detectedUnit == null) {
            for ((tokens, unit) in unitsMap) {
                val matched = tokens.any { token ->
                    Regex("""\b$token\b""").containsMatchIn(text) ||
                            text.split(Regex("""\s+""")).any { FuzzyMatcher.matchesAny(it, listOf(token), 0.85) }
                }
                if (matched) {
                    detectedUnit = unit
                    break
                }
            }
        }

        // 6. ЯКОРЬ ЦЕНЫ: ищем число рядом со словом "рублей / руб / ₽"
        val priceAnchorRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:рубл\w*|руб\.?|₽)""")
        val priceMatch = priceAnchorRegex.find(text)
        if (priceMatch != null) {
            detectedPrice = priceMatch.groupValues[1]
            text = text.replaceRange(priceMatch.range, " ")
        }

        // 7. Распределяем оставшиеся изолированные числа (если слова "рублей" не было)
        val remainingNumbers = Regex("""\b\d+(?:\.\d+)?\b""").findAll(text).map { it.value }.toList()
        for (num in remainingNumbers) {
            if (detectedPrice == null) {
                detectedPrice = num
                text = text.replaceFirst(num, " ")
            } else if (detectedQuantity == null) {
                detectedQuantity = num
                text = text.replaceFirst(num, " ")
            }
        }

        // 8. НАЗВАНИЕ: все оставшиеся смысловые слова
        val stopWords = setOf("цена", "рублей", "рубль", "рубля", "руб", "купи", "за", "по", "товар", "стоит", "возьми", "скидка", "акция", "литр", "литра", "грамм", "килограмм")
        val candidateWords = text.split(Regex("""\s+"""))
            .map { it.filter { char -> char.isLetter() } }
            .filter { it.length >= 3 && it !in stopWords }

        val detectedName = if (candidateWords.isNotEmpty()) {
            candidateWords.joinToString(" ").replaceFirstChar { it.uppercase() }
        } else null

        return ParsedVoiceInput(
            name = detectedName,
            price = detectedPrice,
            quantity = detectedQuantity,
            unit = detectedUnit ?: if (detectedQuantity != null) ProductUnit.GRAM else null,
            discountType = detectedDiscountType,
            discountPercent = detectedDiscountPercent
        )
    }
}
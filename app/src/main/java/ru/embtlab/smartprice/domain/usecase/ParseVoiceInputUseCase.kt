package ru.embtlab.smartprice.domain.usecase

import ru.embtlab.smartprice.domain.model.DiscountType
import ru.embtlab.smartprice.domain.model.ParsedVoiceInput
import ru.embtlab.smartprice.domain.model.ProductUnit
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

        // 2. Преобразование текстовых числительных (от единиц до сотен тысяч и миллионов)
        text = RussianNumberWordParser.parseTextToDigits(text)

        // 3. Склеивание рублей и копеек ("189 рублей 50 копеек", "189 руб 50") -> 189.50
        val rubleKopeckRegex = Regex("""(\d+)\s*(?:рубл\w*|руб\.?|₽)\s*(\d{1,2})\s*(?:коп\w*)?""")
        text = rubleKopeckRegex.replace(text) { match ->
            val rub = match.groupValues[1]
            val kop = match.groupValues[2].padStart(2, '0').take(2)
            "$rub.$kop рублей "
        }

        val onlyKopeckRegex = Regex("""\b(\d{1,2})\s*коп\w*""")
        text = onlyKopeckRegex.replace(text) { match ->
            val kop = match.groupValues[1].padStart(2, '0').take(2)
            "0.$kop рублей "
        }

        val numberAndFractionRegex = Regex("""(\d+)\s+(?:и|точка|целых|целая)\s+(\d{1,2})\b""")
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

        // 5. ЯКОРЬ КОЛИЧЕСТВА: ищем число, стоящее рядом с единицей измерения
        for ((tokens, unit) in unitsMap) {
            val tokensPattern = tokens.joinToString("|") { Regex.escape(it) }
            val unitRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:$tokensPattern)\b|(?:\b(?:$tokensPattern)\s*(\d+(?:\.\d+)?))""")
            val match = unitRegex.find(text)
            if (match != null) {
                detectedQuantity = match.groupValues[1].ifEmpty { match.groupValues[2] }
                detectedUnit = unit
                // Удаляем найденную конструкцию из строки
                text = text.replaceRange(match.range, " ")
                break
            }
        }

        // Если единица измерения прозвучала отдельно без слитного числа
        if (detectedUnit == null) {
            for ((tokens, unit) in unitsMap) {
                val matched = tokens.any { token ->
                    Regex("""(?:\b|(?<=\d))$token\b""").containsMatchIn(text) ||
                            text.split(Regex("""\s+""")).any { FuzzyMatcher.matchesAny(it, listOf(token), 0.82) }
                }
                if (matched) {
                    detectedUnit = unit
                    break
                }
            }
        }

        // 6. ЯКОРЬ ЦЕНЫ: ищем число рядом со словами "рублей", "руб", "₽"
        val priceAnchorRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:рубл\w*|руб\.?|₽)""")
        val priceMatch = priceAnchorRegex.find(text)
        if (priceMatch != null) {
            detectedPrice = priceMatch.groupValues[1]
            text = text.replaceRange(priceMatch.range, " ")
        }

        // 7. Распределяем оставшиеся изолированные числа
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

        // 8. НАЗВАНИЕ: все оставшиеся слова, независимо от их начальной позиции
        val stopWords = setOf("цена", "рублей", "руб", "купи", "за", "по", "товар", "стоит", "возьми", "скидка", "акция")
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
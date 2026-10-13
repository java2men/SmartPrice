package ru.embtlab.smartprice.domain.usecase

import ru.embtlab.smartprice.domain.model.DiscountType
import ru.embtlab.smartprice.domain.model.ParsedVoiceInput
import ru.embtlab.smartprice.domain.model.ProductUnit
import ru.embtlab.smartprice.domain.util.FuzzyMatcher

class ParseVoiceInputUseCase {

    private val unitsMap = listOf(
        listOf("г", "гр", "грамм", "грамма", "граммов", "g", "gr") to ProductUnit.GRAM,
        listOf("кг", "кило", "килограмм", "килограмма", "килограммов", "kg") to ProductUnit.KILOGRAM,
        listOf("мл", "миллилитр", "миллилитра", "миллилитров", "ml") to ProductUnit.MILLILITER,
        listOf("л", "литр", "литра", "литров", "l") to ProductUnit.LITER,
        listOf("шт", "штука", "штуки", "штук", "упаковка", "пачка") to ProductUnit.PIECE
    )

    private val wordReplacements = mapOf(
        "ноль" to "0", "один" to "1", "одна" to "1", "два" to "2", "две" to "2",
        "три" to "3", "четыре" to "4", "пять" to "5", "шесть" to "6", "семь" to "7",
        "восемь" to "8", "девять" to "9", "десять" to "10",
        "двадцать" to "20", "тридцать" to "30", "сорок" to "40", "пятьдесят" to "50",
        "полтора" to "1.5", "полкило" to "500", "пол-литра" to "0.5"
    )

    operator fun invoke(rawSpokenText: String): ParsedVoiceInput {
        if (rawSpokenText.isBlank()) return ParsedVoiceInput()

        var text = rawSpokenText.lowercase().trim().replace(',', '.')

        // Преобразование прописных числительных
        wordReplacements.forEach { (word, replacement) ->
            text = text.replace(Regex("""\b$word\b"""), replacement)
        }

        // 1. Извлечение акций и скидок
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
                    val standalonePercent = Regex("""(\d{1,2})\s*(?:%|процент\w*)""").find(text)
                    if (standalonePercent != null) {
                        detectedDiscountType = DiscountType.PERCENT
                        detectedDiscountPercent = standalonePercent.groupValues[1]
                        text = text.replace(standalonePercent.value, " ")
                    }
                }
            }
        }

        // 2. Поиск единиц измерения
        var detectedUnit: ProductUnit? = null
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

        if (rawSpokenText.contains("полкило", ignoreCase = true) && detectedUnit == null) {
            detectedUnit = ProductUnit.GRAM
        }

        // 3. Извлечение цены и веса (числа скидки уже исключены)
        val numberRegex = Regex("""\b\d+(?:\.\d+)?\b""")
        val allNumbers = numberRegex.findAll(text).map { it.value }.toList()

        var detectedPrice: String? = null
        var detectedQuantity: String? = null

        if (allNumbers.size >= 2) {
            detectedPrice = allNumbers[0]
            detectedQuantity = allNumbers[1]
        } else if (allNumbers.size == 1) {
            val singleNumber = allNumbers[0]
            val isExplicitWeight = detectedUnit != null && (
                    text.contains(Regex("""$singleNumber\s*(г|гр|грамм|кг|мл|л|шт)""")) ||
                            !text.contains(Regex("""(руб|₽|цена|по|за)"""))
                    )

            if (isExplicitWeight && detectedUnit != null) {
                detectedQuantity = singleNumber
            } else {
                detectedPrice = singleNumber
            }
        }

        // 4. Поиск названия товара
        val firstNumIndex = if (allNumbers.isNotEmpty()) text.indexOf(allNumbers.first()) else -1
        val rawPrefix = if (firstNumIndex > 0) text.substring(0, firstNumIndex) else text

        val stopWords = setOf("цена", "рублей", "руб", "купи", "за", "по", "товар", "стоит", "возьми", "скидка", "акция")
        val candidateWords = rawPrefix.split(Regex("""\s+"""))
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
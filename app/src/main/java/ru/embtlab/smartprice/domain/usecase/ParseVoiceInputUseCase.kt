package ru.embtlab.smartprice.domain.usecase

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
        "полтора" to "1.5", "полкило" to "500", "пол-литра" to "0.5"
    )

    operator fun invoke(rawSpokenText: String): ParsedVoiceInput {
        if (rawSpokenText.isBlank()) return ParsedVoiceInput()

        var text = rawSpokenText.lowercase().trim().replace(',', '.')

        // Преобразование текстовых числительных
        wordReplacements.forEach { (word, replacement) ->
            text = text.replace(Regex("""\b$word\b"""), replacement)
        }

        // 1. Поиск единицы измерения
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

        // Если в речи прозвучало слово "полкило", а единица не определилась явно
        if (rawSpokenText.contains("полкило", ignoreCase = true) && detectedUnit == null) {
            detectedUnit = ProductUnit.GRAM
        }

        // 2. Извлечение чисел
        val numberRegex = Regex("""\b\d+(?:\.\d+)?\b""")
        val allNumbers = numberRegex.findAll(text).map { it.value }.toList()

        var detectedPrice: String? = null
        var detectedQuantity: String? = null

        if (allNumbers.size >= 2) {
            // При двух числах: первое — цена, второе — вес
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

        // 3. Выделение названия товара (слова перед числами/валютой)
        val firstNumIndex = if (allNumbers.isNotEmpty()) text.indexOf(allNumbers.first()) else -1
        val rawPrefix = if (firstNumIndex > 0) text.substring(0, firstNumIndex) else text

        val stopWords = setOf("цена", "рублей", "руб", "купи", "за", "по", "товар", "стоит", "возьми")
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
            unit = detectedUnit ?: if (detectedQuantity != null) ProductUnit.GRAM else null
        )
    }
}
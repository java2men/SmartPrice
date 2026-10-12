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

    private val wordNumbers = mapOf(
        "ноль" to 0.0, "один" to 1.0, "одна" to 1.0, "два" to 2.0, "две" to 2.0,
        "три" to 3.0, "четыре" to 4.0, "пять" to 5.0, "шесть" to 6.0, "семь" to 7.0,
        "восемь" to 8.0, "девять" to 9.0, "десять" to 10.0, "полтора" to 1.5, "полкило" to 500.0
    )

    operator fun invoke(rawSpokenText: String): ParsedVoiceInput {
        var cleanText = rawSpokenText.lowercase().trim().replace(',', '.')

        // Преобразование частых слов в числа
        wordNumbers.forEach { (word, value) ->
            cleanText = cleanText.replace(Regex("""\b$word\b"""), if (value % 1.0 == 0.0) value.toInt().toString() else value.toString())
        }

        // Поиск единиц измерения
        var detectedUnit: ProductUnit? = null
        for ((tokens, unit) in unitsMap) {
            val matched = tokens.any { token ->
                Regex("""(?:\b|(?<=\d))$token\b""").containsMatchIn(cleanText) ||
                        cleanText.split(Regex("""\s+""")).any { FuzzyMatcher.matchesAny(it, listOf(token), 0.8) }
            }
            if (matched) {
                detectedUnit = unit
                break
            }
        }

        // Извлечение чисел
        val numberMatches = Regex("""\b\d+(?:\.\d+)?\b""").findAll(cleanText).map { it.value }.toList()

        val price: String? = numberMatches.getOrNull(0)
        val quantity: String? = numberMatches.getOrNull(1)

        // Извлечение возможного названия товара (слова до первого числа)
        val wordsBeforeNumbers = cleanText.substringBefore(price ?: "").trim()
            .split(Regex("""\s+"""))
            .filter { word ->
                word.length > 2 && word !in listOf("цена", "рублей", "руб", "купи", "за", "по", "товар")
            }
        val detectedName = if (wordsBeforeNumbers.isNotEmpty()) {
            wordsBeforeNumbers.joinToString(" ").replaceFirstChar { it.uppercase() }
        } else null

        return ParsedVoiceInput(
            name = detectedName,
            price = price,
            quantity = quantity,
            unit = detectedUnit ?: if (quantity != null) ProductUnit.GRAM else null
        )
    }
}
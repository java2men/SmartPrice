package ru.embtlab.smartprice.domain.util

object RussianNumberWordParser {

    private val numbersMap = mapOf(
        "ноль" to 0L, "нуль" to 0L,
        "один" to 1L, "одна" to 1L, "первый" to 1L, "первая" to 1L,
        "два" to 2L, "две" to 2L, "второй" to 2L, "вторая" to 2L,
        "три" to 3L, "третий" to 3L, "третья" to 3L,
        "четыре" to 4L, "пять" to 5L, "шесть" to 6L, "семь" to 7L,
        "восемь" to 8L, "девять" to 9L,
        "десять" to 10L, "одиннадцать" to 11L, "двенадцать" to 12L, "тринадцать" to 13L,
        "четырнадцать" to 14L, "пятнадцать" to 15L, "шестнадцать" to 16L,
        "семнадцать" to 17L, "восемнадцать" to 18L, "девятнадцать" to 19L,
        "двадцать" to 20L, "тридцать" to 30L, "сорок" to 40L, "пятьдесят" to 50L,
        "шестьдесят" to 60L, "семьдесят" to 70L, "восемьдесят" to 80L, "девяносто" to 90L,
        "сто" to 100L, "двести" to 200L, "триста" to 300L, "четыреста" to 400L,
        "пятьсот" to 500L, "шестьсот" to 600L, "семьсот" to 700L,
        "восемьсот" to 800L, "девятьсот" to 900L
    )

    private val multipliersMap = mapOf(
        "тысяча" to 1000L, "тысячи" to 1000L, "тысяч" to 1000L,
        "миллион" to 1000000L, "миллиона" to 1000000L, "миллионов" to 1000000L
    )

    fun parseTextToDigits(input: String): String {
        if (input.isBlank()) return ""

        val rawWords = input.split(Regex("""\s+""")).filter { it.isNotBlank() }
        val outputTokens = mutableListOf<String>()

        var currentSection = 0L
        var total = 0L
        var inNumberSequence = false

        for (word in rawWords) {
            val cleanWord = word.filter { it.isLetter() }
            val num = numbersMap[cleanWord]
            val mult = multipliersMap[cleanWord]

            when {
                num != null -> {
                    currentSection += num
                    inNumberSequence = true
                }
                mult != null -> {
                    if (currentSection == 0L) currentSection = 1L
                    total += currentSection * mult
                    currentSection = 0L
                    inNumberSequence = true
                }
                else -> {
                    if (inNumberSequence) {
                        total += currentSection
                        outputTokens.add(total.toString())
                        total = 0L
                        currentSection = 0L
                        inNumberSequence = false
                    }
                    outputTokens.add(word)
                }
            }
        }

        if (inNumberSequence) {
            total += currentSection
            outputTokens.add(total.toString())
        }

        return outputTokens.joinToString(" ")
    }
}
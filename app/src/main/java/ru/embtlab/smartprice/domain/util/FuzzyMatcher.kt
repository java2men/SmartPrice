package ru.embtlab.smartprice.domain.util

object FuzzyMatcher {

    /**
     * Расстояние Левенштейна (минимальное число односимвольных правок: вставка, удаление, замена).
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1].equals(s2[j - 1], ignoreCase = true)) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Степень похожести строк от 0.0 до 1.0 (1.0 = идентичны).
     */
    fun similarity(s1: String, s2: String): Double {
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1.0
        val dist = levenshteinDistance(s1.trim().lowercase(), s2.trim().lowercase())
        return 1.0 - (dist.toDouble() / maxLen)
    }

    /**
     * Проверяет, похоже ли слово token на любое слово из списка dictionary с порогом threshold.
     */
    fun matchesAny(token: String, dictionary: List<String>, threshold: Double = 0.72): Boolean {
        val clean = token.trim().lowercase()
        if (clean.isEmpty()) return false
        return dictionary.any { dictWord ->
            if (clean == dictWord) true
            else similarity(clean, dictWord) >= threshold
        }
    }
}
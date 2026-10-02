package ru.embtlab.smartprice.presentation.compare.util

object InputFormatters {

    fun sanitizePrice(input: String, previousValue: String = ""): String {
        val sanitized = input.replace(',', '.')
        if (sanitized.isEmpty()) return ""
        if (sanitized.count { it == '.' } > 1) return previousValue

        val parts = sanitized.split('.')
        val integerPart = parts[0]
        val fractionalPart = parts.getOrNull(1)

        // Ограничение: максимум 6 цифр до запятой и максимум 2 после
        if (integerPart.length > 6) return previousValue
        if (fractionalPart != null && fractionalPart.length > 2) return previousValue

        val regex = Regex("""^\d{0,6}(\.\d{0,2})?$""")
        return if (regex.matches(sanitized)) sanitized else previousValue
    }

    fun sanitizeQuantity(input: String, previousValue: String = ""): String {
        val sanitized = input.replace(',', '.')
        if (sanitized.isEmpty()) return ""
        if (sanitized.count { it == '.' } > 1) return previousValue

        val parts = sanitized.split('.')
        val integerPart = parts[0]
        val fractionalPart = parts.getOrNull(1)

        // Ограничение: максимум 5 цифр до запятой и максимум 3 после (для кг/л)
        if (integerPart.length > 5) return previousValue
        if (fractionalPart != null && fractionalPart.length > 3) return previousValue

        val regex = Regex("""^\d{0,5}(\.\d{0,3})?$""")
        return if (regex.matches(sanitized)) sanitized else previousValue
    }

    fun sanitizeDiscount(input: String, previousValue: String = ""): String {
        val sanitized = input.replace(',', '.')
        if (sanitized.isEmpty()) return ""
        if (sanitized.count { it == '.' } > 1) return previousValue

        val parts = sanitized.split('.')
        val integerPart = parts[0]
        val fractionalPart = parts.getOrNull(1)

        if (integerPart.length > 2) return previousValue
        if (fractionalPart != null && fractionalPart.length > 1) return previousValue

        val regex = Regex("""^\d{0,2}(\.\d{0,1})?$""")
        return if (regex.matches(sanitized)) sanitized else previousValue
    }
}
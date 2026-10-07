package ru.embtlab.smartprice.presentation.compare.util

import ru.embtlab.smartprice.domain.model.ProductConstants

object InputFormatters {
    private val priceRegex = Regex("""^\d{0,${ProductConstants.MAX_PRICE_INTEGER_DIGITS}}(\.\d{0,${ProductConstants.MAX_PRICE_DECIMAL_DIGITS}})?$""")
    private val quantityRegex = Regex("""^\d{0,${ProductConstants.MAX_QUANTITY_INTEGER_DIGITS}}(\.\d{0,${ProductConstants.MAX_QUANTITY_DECIMAL_DIGITS}})?$""")
    private val discountRegex = Regex("""^\d{0,2}(\.\d{0,1})?$""")

    fun sanitizePrice(input: String, previousValue: String = ""): String {
        val sanitized = input.replace(',', '.')
        if (sanitized.isEmpty()) return ""
        if (sanitized.count { it == '.' } > 1) return previousValue
        val parts = sanitized.split('.')
        val integerPart = parts[0]
        val fractionalPart = parts.getOrNull(1)

        if (integerPart.length > ProductConstants.MAX_PRICE_INTEGER_DIGITS) return previousValue
        if (fractionalPart != null && fractionalPart.length > ProductConstants.MAX_PRICE_DECIMAL_DIGITS) return previousValue

        return if (priceRegex.matches(sanitized)) sanitized else previousValue
    }

    fun sanitizeQuantity(input: String, previousValue: String = ""): String {
        val sanitized = input.replace(',', '.')
        if (sanitized.isEmpty()) return ""
        if (sanitized.count { it == '.' } > 1) return previousValue
        val parts = sanitized.split('.')
        val integerPart = parts[0]
        val fractionalPart = parts.getOrNull(1)

        if (integerPart.length > ProductConstants.MAX_QUANTITY_INTEGER_DIGITS) return previousValue
        if (fractionalPart != null && fractionalPart.length > ProductConstants.MAX_QUANTITY_DECIMAL_DIGITS) return previousValue

        return if (quantityRegex.matches(sanitized)) sanitized else previousValue
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

        return if (discountRegex.matches(sanitized)) sanitized else previousValue
    }
}
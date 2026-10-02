package ru.embtlab.smartprice.domain.usecase

import ru.embtlab.smartprice.domain.model.ParsedPriceTag
import ru.embtlab.smartprice.domain.model.ProductUnit

class ParsePriceTagUseCase {

    private val priceRegex = Regex("""(\d{1,5}[.,]\d{2})""")
    private val weightGramRegex = Regex("""(\d{2,4})\s*(?:г|g|гр)\b""", RegexOption.IGNORE_CASE)
    private val weightKgRegex = Regex("""(\d{1,2}(?:[.,]\d{1,3})?)\s*(?:кг|kg)\b""", RegexOption.IGNORE_CASE)
    private val volumeMlRegex = Regex("""(\d{2,4})\s*(?:мл|ml)\b""", RegexOption.IGNORE_CASE)
    private val volumeLiterRegex = Regex("""(\d{1,2}(?:[.,]\d{1,3})?)\s*(?:л|l)\b""", RegexOption.IGNORE_CASE)

    operator fun invoke(rawText: String): ParsedPriceTag {
        // 1. Поиск цены (формат XXX.XX или XXX,XX)
        val priceMatch = priceRegex.findAll(rawText)
            .map { it.groupValues[1].replace(',', '.') }
            .firstOrNull()

        // 2. Поиск веса / объема
        val gramMatch = weightGramRegex.find(rawText)?.groupValues?.get(1)
        if (gramMatch != null) {
            return ParsedPriceTag(price = priceMatch, quantity = gramMatch, unit = ProductUnit.GRAM)
        }

        val kgMatch = weightKgRegex.find(rawText)?.groupValues?.get(1)?.replace(',', '.')
        if (kgMatch != null) {
            return ParsedPriceTag(price = priceMatch, quantity = kgMatch, unit = ProductUnit.KILOGRAM)
        }

        val mlMatch = volumeMlRegex.find(rawText)?.groupValues?.get(1)
        if (mlMatch != null) {
            return ParsedPriceTag(price = priceMatch, quantity = mlMatch, unit = ProductUnit.MILLILITER)
        }

        val literMatch = volumeLiterRegex.find(rawText)?.groupValues?.get(1)?.replace(',', '.')
        if (literMatch != null) {
            return ParsedPriceTag(price = priceMatch, quantity = literMatch, unit = ProductUnit.LITER)
        }

        return ParsedPriceTag(price = priceMatch, quantity = null, unit = null)
    }
}
package ru.embtlab.smartprice.domain.model

enum class UnitCategory {
    WEIGHT, VOLUME, PIECES
}

enum class ProductUnit(
    val label: String,
    val category: UnitCategory,
    val factorToBase: Double
) {
    GRAM("г", UnitCategory.WEIGHT, 0.001),
    KILOGRAM("кг", UnitCategory.WEIGHT, 1.0),
    MILLILITER("мл", UnitCategory.VOLUME, 0.001),
    LITER("л", UnitCategory.VOLUME, 1.0),
    PIECE("шт", UnitCategory.PIECES, 1.0),
    PACKAGE("упак", UnitCategory.PIECES, 1.0)
}
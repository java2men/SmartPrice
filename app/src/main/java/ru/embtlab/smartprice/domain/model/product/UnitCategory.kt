package ru.embtlab.smartprice.domain.model.product

enum class UnitCategory(val title: String, val baseLabel: String) {
    WEIGHT("Вес", "кг"),
    VOLUME("Объём", "л"),
    LENGTH("Длина", "м"),
    AREA("Площадь", "м²"),
    PACKAGING("Фасовка и штуки", "шт")
}
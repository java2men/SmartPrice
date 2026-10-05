// domain/model/ProductUnit.kt
package ru.embtlab.smartprice.domain.model

enum class UnitCategory(val title: String, val baseLabel: String) {
    WEIGHT("Вес", "кг"),
    VOLUME("Объём", "л"),
    PIECES("Штуки и фасовка", "шт"),
    DOSES("Таблетки и стирки", "доза"),
    ROLLS("Рулоны и салфетки", "рул"),
    METERS("Длина и площадь", "м")
}

enum class ProductUnit(
    val label: String,
    val fullName: String,
    val category: UnitCategory,
    val factorToBase: Double,
    val searchTags: List<String>
) {
    // Вес
    GRAM("г", "Граммы", UnitCategory.WEIGHT, 0.001, listOf("грамм", "гр", "г")),
    KILOGRAM("кг", "Килограммы", UnitCategory.WEIGHT, 1.0, listOf("килограмм", "кило", "кг")),

    // Объём
    MILLILITER("мл", "Миллилитры", UnitCategory.VOLUME, 0.001, listOf("миллилитр", "мл")),
    LITER("л", "Литры", UnitCategory.VOLUME, 1.0, listOf("литр", "л")),

    // Штучный товар
    PIECE("шт", "Штуки", UnitCategory.PIECES, 1.0, listOf("штука", "шт", "яйца", "фрукты")),
    PACKAGE("упак", "Упаковки", UnitCategory.PIECES, 1.0, listOf("упаковка", "пачка", "упак")),
    SACHET("пак", "Пакетики / саше", UnitCategory.PIECES, 1.0, listOf("пакетик", "саше", "чай", "кофе", "пауч")),

    // Бытовая химия и медицина
    TABLET("таб", "Таблетки", UnitCategory.DOSES, 1.0, listOf("таблетка", "таб", "посудомойка")),
    CAPSULE("капс", "Капсулы", UnitCategory.DOSES, 1.0, listOf("капсула", "капс", "витамины")),
    WASHES("стир", "Стирки / дозы", UnitCategory.DOSES, 1.0, listOf("стирка", "гель", "порошок", "концентрат", "доза")),

    // Рулоны и бумага
    ROLL("рул", "Рулоны", UnitCategory.ROLLS, 1.0, listOf("рулон", "бумага", "полотенца", "пакеты")),
    SHEET("лист", "Листы", UnitCategory.ROLLS, 1.0, listOf("лист", "салфетки")),

    // Метраж
    METER("м", "Метры", UnitCategory.METERS, 1.0, listOf("метр", "пленка", "фольга", "рукав", "кабель")),
    SQ_METER("м²", "Кв. метры", UnitCategory.METERS, 1.0, listOf("квадратный метр", "плитка", "ламинат"))
}
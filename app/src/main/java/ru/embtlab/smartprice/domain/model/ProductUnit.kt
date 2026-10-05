package ru.embtlab.smartprice.domain.model

enum class UnitCategory(val title: String, val baseLabel: String) {
    WEIGHT("Вес", "кг"),
    VOLUME("Объём", "л"),
    LENGTH("Длина", "м"),
    AREA("Площадь", "м²"),
    PACKAGING("Фасовка и штуки", "шт")
}

enum class ProductUnit(
    val label: String,
    val fullName: String,
    val category: UnitCategory,
    val factorToBase: Double,
    val searchTags: List<String>
) {
    // ================= ВЕС (База: 1 кг) =================
    MILLIGRAM("мг", "Миллиграммы", UnitCategory.WEIGHT, 0.000001, listOf("миллиграмм", "мг", "специи", "лекарства")),
    GRAM("г", "Граммы", UnitCategory.WEIGHT, 0.001, listOf("грамм", "гр", "г")),
    KILOGRAM("кг", "Килограммы", UnitCategory.WEIGHT, 1.0, listOf("килограмм", "кило", "кг")),
    CENTNER("ц", "Центнеры", UnitCategory.WEIGHT, 100.0, listOf("центнер", "ц", "мешок", "сахар", "мука")),
    TON("т", "Тонны", UnitCategory.WEIGHT, 1000.0, listOf("тонна", "т")),

    // ================= ОБЪЁМ (База: 1 л) =================
    MILLILITER("мл", "Миллилитры", UnitCategory.VOLUME, 0.001, listOf("миллилитр", "мл")),
    CENTILITER("сл", "Сантилитры", UnitCategory.VOLUME, 0.01, listOf("сантилитр", "сл", "алкоголь")),
    DECILITER("дл", "Децилитры", UnitCategory.VOLUME, 0.1, listOf("децилитр", "дл")),
    LITER("л", "Литры", UnitCategory.VOLUME, 1.0, listOf("литр", "л")),
    CUBIC_METER("м³", "Куб. метры", UnitCategory.VOLUME, 1000.0, listOf("кубометр", "куб", "м3", "вода", "дрова", "газ")),

    // ================= ДЛИНА (База: 1 м) =================
    MILLIMETER("мм", "Миллиметры", UnitCategory.LENGTH, 0.001, listOf("миллиметр", "мм", "провод")),
    CENTIMETER("см", "Сантиметры", UnitCategory.LENGTH, 0.01, listOf("сантиметр", "см", "лента", "ткань")),
    DECIMETER("дм", "Дециметры", UnitCategory.LENGTH, 0.1, listOf("дециметр", "дм")),
    METER("м", "Метры", UnitCategory.LENGTH, 1.0, listOf("метр", "м", "пленка", "фольга", "кабель", "рукав", "обои")),
    KILOMETER("км", "Километры", UnitCategory.LENGTH, 1000.0, listOf("километр", "км")),

    // ================= ПЛОЩАДЬ (База: 1 м²) =================
    SQ_CENTIMETER("см²", "Кв. сантиметры", UnitCategory.AREA, 0.0001, listOf("квадратный сантиметр", "см2")),
    SQ_DECIMETER("дм²", "Кв. дециметры", UnitCategory.AREA, 0.01, listOf("квадратный дециметр", "дм2", "кожа")),
    SQ_METER("м²", "Кв. метры", UnitCategory.AREA, 1.0, listOf("квадратный метр", "м2", "плитка", "ламинат", "гипсокартон")),
    ARE("сотка / ар", "Сотки (а)", UnitCategory.AREA, 100.0, listOf("сотка", "ар", "земля", "газон")),

    // ================= ФАСОВКА И ШТУКИ (База: 1 шт, в самом низу) =================
    PIECE("шт", "Штуки", UnitCategory.PACKAGING, 1.0, listOf("штука", "шт", "яйца", "фрукты", "овощи")),
    PACKAGE("упак", "Упаковки", UnitCategory.PACKAGING, 1.0, listOf("упаковка", "пачка", "упак")),
    TABLET("таб", "Таблетки", UnitCategory.PACKAGING, 1.0, listOf("таблетка", "таб", "посудомойка", "лекарство")),
    CAPSULE("капс", "Капсулы", UnitCategory.PACKAGING, 1.0, listOf("капсула", "капс", "стирка", "витамины")),
    WASHES("стир / доз", "Стирки / дозы", UnitCategory.PACKAGING, 1.0, listOf("стирка", "гель", "порошок", "концентрат", "доза")),
    SACHET("пак", "Пакетики / саше", UnitCategory.PACKAGING, 1.0, listOf("пакетик", "саше", "чай", "кофе", "пауч")),
    NAPKIN("салф", "Салфетки / платки", UnitCategory.PACKAGING, 1.0, listOf("салфетка", "салфетки", "платочки")),
    SHEET("лист", "Листы", UnitCategory.PACKAGING, 1.0, listOf("лист", "бумага", "пергамент")),
    ROLL("рул", "Рулоны", UnitCategory.PACKAGING, 1.0, listOf("рулон", "бумага", "полотенца", "пакеты", "мусорные пакеты"))
}
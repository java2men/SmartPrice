package ru.embtlab.smartprice.domain.model

enum class CardStylePreset(val title: String, val description: String) {
    CLASSIC(
        title = "Классический сплит",
        description = "Привычные раздельные поля цены и количества"
    ),
    SMART_SINGLE_FIELD(
        title = "Экспресс-ввод (1 строка)",
        description = "Быстрый ввод «Цена Кол-во» в одну строку"
    ),
    KEYPAD_PRESETS(
        title = "Калькулятор с пресетами",
        description = "Крупные цифры и быстрые кнопки объёмов"
    ),
    FULL_SCREEN_EDITOR(
        title = "Компактный список (редактор на экране)",
        description = "Лаконичные карточки результатов, детальный ввод на отдельном экране"
    )
}
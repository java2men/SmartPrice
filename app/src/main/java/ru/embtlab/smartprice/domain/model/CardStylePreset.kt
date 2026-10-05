package ru.embtlab.smartprice.domain.model

enum class CardStylePreset(val title: String, val description: String) {
    CLASSIC(
        title = "Классический сплит",
        description = "Привычные раздельные поля цены и количества в едином контуре"
    ),
    SMART_SINGLE_FIELD(
        title = "Экспресс-ввод (1 строка)",
        description = "Быстрый ввод связки «Цена Вес» через пробел или слэш (например, 189 900)"
    ),
    KEYPAD_PRESETS(
        title = "Калькулятор с пресетами",
        description = "Крупные цифры и быстрые кнопки объёмов (400г, 900г, 1л) без клавиатуры"
    )
}
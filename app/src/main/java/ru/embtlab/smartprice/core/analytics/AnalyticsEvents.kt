package ru.embtlab.smartprice.core.analytics

object AnalyticsEvents {

    // --- 1. Основной флоу расчетов (Core Loop) ---
    const val COMPARISON_CALCULATED = "comparison_calculated"
    const val COMPARISON_SAVED = "comparison_saved"
    const val COMPARISON_RESET = "comparison_reset"
    const val PRODUCT_ADDED = "product_added"
    const val PRODUCT_REMOVED = "product_removed"
    const val INCOMPATIBLE_UNITS_WARNING = "incompatible_units_warning"

    // --- 2. Распознавание ценников камерой (OCR) ---
    const val OCR_SCANNER_OPENED = "ocr_scanner_opened"
    const val OCR_FRAME_CAPTURED = "ocr_frame_captured"
    const val OCR_DATA_APPLIED = "ocr_data_applied"
    const val OCR_SCANNER_CLOSED = "ocr_scanner_closed"

    // --- 3. Голосовой ввод (Voice PTT) ---
    const val VOICE_INPUT_STARTED = "voice_input_started"
    const val VOICE_INPUT_COMPLETED = "voice_input_completed"
    const val VOICE_INPUT_CANCELLED = "voice_input_cancelled"

    // --- 4. История и Настройки ---
    const val HISTORY_RESTORED = "history_restored"
    const val HISTORY_ITEM_DELETED = "history_item_deleted"
    const val PRESET_CHANGED = "preset_changed"

    // --- Параметры событий (Event Params) ---
    object Params {
        const val ITEMS_COUNT = "items_count"
        const val UNIT_CATEGORY = "unit_category"
        const val SAVINGS_PERCENT = "savings_percent"
        const val HAS_DISCOUNT = "has_discount"
        const val HAS_CUSTOM_NAME = "has_custom_name"
        const val SUCCESS = "success"
        const val INPUT_MODE = "input_mode" // manual / chip / numpad
        const val PRESET_NAME = "preset_name"
        const val DURATION_SEC = "duration_sec"
        const val SOURCE = "source"
    }
}
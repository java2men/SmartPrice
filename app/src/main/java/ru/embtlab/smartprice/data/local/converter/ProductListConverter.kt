package ru.embtlab.smartprice.data.local.converter

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject
import ru.embtlab.smartprice.domain.model.DiscountType
import ru.embtlab.smartprice.domain.model.ProductItem
import ru.embtlab.smartprice.domain.model.ProductUnit

class ProductListConverter {

    @TypeConverter
    fun fromProductList(items: List<ProductItem>?): String {
        if (items.isNullOrEmpty()) return "[]"
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("priceInput", item.priceInput)
                put("quantityInput", item.quantityInput)
                put("unit", item.unit.name)
                put("discountType", item.discountType.name)
                put("customDiscountPercentInput", item.customDiscountPercentInput)
            }
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toProductList(jsonString: String?): List<ProductItem> {
        if (jsonString.isNullOrBlank()) return emptyList()
        val list = mutableListOf<ProductItem>()
        val array = JSONArray(jsonString)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ProductItem(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    priceInput = obj.optString("priceInput"),
                    quantityInput = obj.optString("quantityInput"),
                    unit = runCatching { ProductUnit.valueOf(obj.optString("unit")) }.getOrDefault(ProductUnit.GRAM),
                    discountType = runCatching { DiscountType.valueOf(obj.optString("discountType")) }.getOrDefault(DiscountType.NONE),
                    customDiscountPercentInput = obj.optString("customDiscountPercentInput")
                )
            )
        }
        return list
    }
}
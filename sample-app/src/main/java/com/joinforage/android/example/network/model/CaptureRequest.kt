package com.joinforage.android.example.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CaptureRequest(
    @Json(name = "capture_amount") val captureAmount: String,
    @Json(name = "qualified_healthcare_total") val qualifiedHealthcareTotal: String,
    @Json(name = "qualified_healthcare_subtotal") val qualifiedHealthcareSubtotal: String,
    @Json(name = "product_list") val productList: List<Product>
) {
    @JsonClass(generateAdapter = true)
    data class Product(
        val gtin: String,
        val name: String,
        @Json(name = "unit_price") val unitPrice: String,
        val quantity: String
    )
}

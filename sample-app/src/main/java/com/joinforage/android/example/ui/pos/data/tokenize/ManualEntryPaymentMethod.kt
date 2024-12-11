import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ManualEntryPaymentMethod(
    val card: Card,
    val type: String = "ebt",
    val reusable: Boolean = true,
    @Json(name = "customer_id") val customerId: String? = null
) {
    @JsonClass(generateAdapter = true)
    data class Card(
        @Json(name = "number") val cardNumber: String
    )

    constructor(cardNumber: String) : this(Card(cardNumber))
}

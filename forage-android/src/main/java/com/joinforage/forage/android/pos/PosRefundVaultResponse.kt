package com.joinforage.forage.android.pos

import com.joinforage.forage.android.network.model.Message
import org.json.JSONObject

/**
 * Shape of response from the vault proxy when refunding a payment.
 * @property message the SQS Message.
 * @property refundRef the reference string to the refund that was created.
 */
internal data class PosRefundVaultResponse(
    val message: Message,
    val refundRef: String
) {
    object ModelMapper {
        fun from(string: String): PosRefundVaultResponse {
            val jsonObject = JSONObject(string)

            val messageJsonObject = jsonObject.getJSONObject("message")

            val message = Message.ModelMapper.from(messageJsonObject.toString())
            val refundRef = jsonObject.getString("ref")

            return PosRefundVaultResponse(
                message = message,
                refundRef = refundRef
            )
        }
    }
}

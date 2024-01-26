package com.joinforage.forage.android.mock

import com.joinforage.forage.android.fixtures.givenContentId
import com.joinforage.forage.android.fixtures.givenEncryptionKey
import com.joinforage.forage.android.fixtures.givenPaymentAndRefundRef
import com.joinforage.forage.android.fixtures.givenPaymentMethodRef
import com.joinforage.forage.android.fixtures.givenPaymentRef
import com.joinforage.forage.android.fixtures.returnsEncryptionKeySuccessfully
import com.joinforage.forage.android.fixtures.returnsMessageCompletedSuccessfully
import com.joinforage.forage.android.fixtures.returnsPayment
import com.joinforage.forage.android.fixtures.returnsPaymentMethod
import com.joinforage.forage.android.fixtures.returnsRefund
import com.joinforage.forage.android.network.model.ForageApiResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject

// contains the minimal data needed to marshall a refund response into a PosRefundVaultResponse
internal val MOCK_VAULT_REFUND_RESPONSE = """
{
  "ref": "${MockServiceFactory.ExpectedData.refundRef}",                
  "message": {
    "content_id": "${MockServiceFactory.ExpectedData.contentId}",
    "message_type": "0200",
    "status": "sent_to_proxy",
    "failed": false,
    "errors": []
  }
}
""".trimIndent()

internal fun mockSuccessfulPosRefund(
    mockVaultSubmitter: MockVaultSubmitter,
    server: MockWebServer
) {
    server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
    server.givenPaymentRef().returnsPayment()
    server.givenPaymentMethodRef().returnsPaymentMethod()
    server.givenContentId(MockServiceFactory.ExpectedData.contentId)
        .returnsMessageCompletedSuccessfully()
    server.givenPaymentAndRefundRef().returnsRefund()

    mockVaultSubmitter.setSubmitResponse(
        path = "/api/payments/${MockServiceFactory.ExpectedData.paymentRef}/refunds/",
        response = ForageApiResponse.Success(MOCK_VAULT_REFUND_RESPONSE)
    )
}

internal fun getVaultMessageResponse(contentId: String): String {
    return JSONObject().apply {
        put("content_id", contentId)
        put("message_type", "0200")
        put("status", "sent_to_proxy")
        put("failed", false)
        put("errors", JSONArray(emptyList<String>()))
    }.toString()
}

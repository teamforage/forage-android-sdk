package com.joinforage.forage.android.network.mapper

import com.joinforage.forage.android.network.model.Response
import com.verygoodsecurity.vgscollect.core.model.network.VGSResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ToResponseTest {
    @Test
    fun `it should map the successful VGS response`() {
        val rawResponse =
            "{\"ref\":\"6bfdeecae7\",\"merchant\":\"8000009\",\"funding_type\":\"ebt_snap\",\"amount\":\"1.00\",\"description\":\"desc\",\"metadata\":{},\"payment_method\":\"f232c0f37b\",\"delivery_address\":{\"city\":\"Los Angeles\",\"country\":\"United States\",\"line1\":\"Street\",\"line2\":\"Number\",\"state\":\"LA\",\"zipcode\":\"12345\"},\"is_delivery\":false,\"created\":\"2022-11-03T05:30:15.710618-07:00\",\"updated\":\"2022-11-03T05:30:28.325187-07:00\",\"status\":\"succeeded\",\"last_processing_error\":null,\"success_date\":\"2022-11-03T12:30:28.322112Z\",\"refunds\":[]}"
        val vgsResponse = VGSResponse.SuccessResponse(
            rawResponse = rawResponse,
            successCode = 200
        )

        val mappedResponse = vgsResponse.toResponse()
        assertThat(mappedResponse).isExactlyInstanceOf(Response.SuccessResponse::class.java)
        val response = mappedResponse as Response.SuccessResponse
        assertThat(response).isEqualTo(
            Response.SuccessResponse(
                successCode = 200,
                rawResponse = rawResponse
            )
        )
    }

    @Test
    fun `it should map the error VGS response`() {
        val rawResponse =
            "{\"path\":\"/api/payments/10483aac8c/capture/\",\"errors\":[{\"code\":\"ebt_error_51\",\"message\":\"Insufficient funds - Insufficient Funds. Remaining balances are SNAP: N/A, EBT Cash: \$98.00\",\"source\":{\"resource\":\"Payments\",\"ref\":\"10483aac8c\"}}]}"
        val vgsResponse = VGSResponse.ErrorResponse(
            rawResponse = rawResponse,
            errorCode = 400,
            localizeMessage = ""
        )

        val mappedResponse = vgsResponse.toResponse()
        assertThat(mappedResponse).isExactlyInstanceOf(Response.ErrorResponse::class.java)
        val response = mappedResponse as Response.ErrorResponse
        assertThat(response).isEqualTo(
            Response.ErrorResponse(
                rawResponse = rawResponse,
                localizeMessage = "",
                errorCode = 400
            )
        )
    }
}

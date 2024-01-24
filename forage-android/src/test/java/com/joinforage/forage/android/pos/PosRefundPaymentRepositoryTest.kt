package com.joinforage.forage.android.pos

import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.fixtures.givenContentId
import com.joinforage.forage.android.fixtures.givenEncryptionKey
import com.joinforage.forage.android.fixtures.givenPaymentAndRefundRef
import com.joinforage.forage.android.fixtures.givenPaymentMethodRef
import com.joinforage.forage.android.fixtures.givenPaymentRef
import com.joinforage.forage.android.fixtures.returnsEncryptionKeySuccessfully
import com.joinforage.forage.android.fixtures.returnsFailedPayment
import com.joinforage.forage.android.fixtures.returnsFailedPaymentMethod
import com.joinforage.forage.android.fixtures.returnsFailedRefund
import com.joinforage.forage.android.fixtures.returnsMessageCompletedSuccessfully
import com.joinforage.forage.android.fixtures.returnsPayment
import com.joinforage.forage.android.fixtures.returnsPaymentMethod
import com.joinforage.forage.android.fixtures.returnsRefund
import com.joinforage.forage.android.fixtures.returnsUnauthorizedEncryptionKey
import com.joinforage.forage.android.mock.MockRepositoryFactory
import com.joinforage.forage.android.network.data.TestVaultSubmitter
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.ui.ForagePINEditText
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class PosRefundPaymentRepositoryTest : MockServerSuite() {
    private lateinit var repository: PosRefundPaymentRepository
    private val vaultSubmitter = TestVaultSubmitter(VaultType.VGS_VAULT_TYPE)
    private val expectedData = MockRepositoryFactory.ExpectedData
    private lateinit var mockForagePinEditText: ForagePINEditText

    @Before
    override fun setup() {
        super.setup()

        mockForagePinEditText = mock(ForagePINEditText::class.java)
        val logger = Log.getSilentInstance()
        repository = MockRepositoryFactory(
            logger = logger,
            server = server
        ).createPosRefundPaymentRepository(vaultSubmitter)
    }

    private suspend fun executeRefundPayment(): ForageApiResponse<String> {
        return repository.refundPayment(
            merchantId = expectedData.merchantId,
            posTerminalId = expectedData.posTerminalId,
            refundParams = PosRefundPaymentParams(
                foragePinEditText = mockForagePinEditText,
                paymentRef = expectedData.paymentRef,
                amount = 1.0f,
                reason = "I feel like refunding!",
                metadata = hashMapOf("meta" to "verse", "my_store_location_id" to "123456")
            )
        )
    }

    private fun setVaultResponse(response: ForageApiResponse<String>) {
        vaultSubmitter.setSubmitResponse(
            params = TestVaultSubmitter.RequestContainer(
                merchantId = expectedData.merchantId,
                path = "/api/payments/${expectedData.paymentRef}/refunds/",
                paymentMethodRef = expectedData.paymentMethodRef,
                idempotencyKey = expectedData.paymentRef
            ),
            response = response
        )
    }

    @Test
    fun `it should return a failure when the getting the encryption key fails`() = runTest {
        server.givenEncryptionKey().returnsUnauthorizedEncryptionKey()

        val response = executeRefundPayment()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val clientError = response as ForageApiResponse.Failure

        assertThat(clientError.errors[0].message).contains("Authentication credentials were not provided.")
    }

    @Test
    fun `it should return a failure when the get payment returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsFailedPayment()

        val response = executeRefundPayment()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val failureResponse = response as ForageApiResponse.Failure
        val expectedMessage = "Cannot find payment."
        val expectedForageCode = "not_found"
        val expectedStatusCode = 404
        assertThat(failureResponse.errors[0].message).isEqualTo(expectedMessage)
        assertThat(failureResponse.errors[0].code).isEqualTo(expectedForageCode)
        assertThat(failureResponse.errors[0].httpStatusCode).isEqualTo(expectedStatusCode)
    }

    @Test
    fun `it should return a failure when the get payment method returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsFailedPaymentMethod()

        val response = executeRefundPayment()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val failureResponse = response as ForageApiResponse.Failure
        val expectedMessage = "EBT Card could not be found"
        val expectedForageCode = "not_found"
        val expectedStatusCode = 404

        assertThat(failureResponse.errors[0].message).isEqualTo(expectedMessage)
        assertThat(failureResponse.errors[0].code).isEqualTo(expectedForageCode)
        assertThat(failureResponse.errors[0].httpStatusCode).isEqualTo(expectedStatusCode)
    }

    @Test
    fun `it should fail if getting the refund fails`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        server.givenContentId(expectedData.contentId)
            .returnsMessageCompletedSuccessfully()
        server.givenPaymentAndRefundRef().returnsFailedRefund()

        setVaultResponse(ForageApiResponse.Success(mockVaultRefundResponse.trimIndent()))

        val expectedMessage = "Refund with ref refund123 does not exist for current Merchant with FNS 1234567."
        val expectedCode = "resource_not_found"
        val expectedStatusCode = 404

        val response = executeRefundPayment()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val failureResponse = response as ForageApiResponse.Failure
        val firstError = failureResponse.errors[0]
        assertThat(firstError.message).isEqualTo(expectedMessage)
        assertThat(firstError.code).isEqualTo(expectedCode)
        assertThat(firstError.httpStatusCode).isEqualTo(expectedStatusCode)
    }

    @Test
    fun `it should fail on vault proxy pin submission`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsPaymentMethod()

        val expectedMessage = "Only Payments in the succeeded state can be refunded, but Payment with ref abcdef123 is in the canceled state"
        val expectedForageCode = "cannot_refund_payment"
        val expectedStatusCode = 400
        setVaultResponse(
            ForageApiResponse.Failure.fromError(
                ForageError(400, code = expectedForageCode, message = expectedMessage)
            )
        )

        val response = executeRefundPayment()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val failureResponse = response as ForageApiResponse.Failure
        assertThat(failureResponse.errors[0].message).isEqualTo(expectedMessage)
        assertThat(failureResponse.errors[0].code).isEqualTo(expectedForageCode)
        assertThat(failureResponse.errors[0].httpStatusCode).isEqualTo(expectedStatusCode)
    }

    @Test
    fun `it should succeed`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        server.givenContentId(expectedData.contentId)
            .returnsMessageCompletedSuccessfully()
        server.givenPaymentAndRefundRef().returnsRefund()

        setVaultResponse(ForageApiResponse.Success(mockVaultRefundResponse.trimIndent()))

        val response = executeRefundPayment()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Success::class.java)
        when (response) {
            is ForageApiResponse.Success -> {
                assertEquals(expectedData.refundRef, JSONObject(response.data).getString("ref"))
            }
            else -> {
                assertThat(false)
            }
        }
    }

    companion object {
        private val expectedData = MockRepositoryFactory.ExpectedData
        private val mockVaultRefundResponse = """
                {
                  "ref": "${expectedData.refundRef}",
                  "payment_ref": "${expectedData.paymentRef}",
                  "funding_type": "ebt_snap",
                  "amount": "1.00",
                  "reason": "I feel like it!",
                  "metadata": {
                      "meta": "verse",
                      "my_store_location_id": "123456"
                  },
                  "created": "2024-01-24T10:32:38.709135-08:00",
                  "updated": "2024-01-24T10:32:38.862584-08:00",
                  "status": "processing",
                  "last_processing_error": null,
                  "receipt": null,
                  "pos_terminal": {
                    "terminal_id": "terminal123",
                    "provider_terminal_id": "pos-terminal-123"
                  },
                  "external_order_id": null,
                  "message": {
                    "content_id": "${expectedData.contentId}",
                    "message_type": "0200",
                    "status": "sent_to_proxy",
                    "failed": false,
                    "errors": []
                  }
                }
        """.trimIndent()
    }
}

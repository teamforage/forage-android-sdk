package com.joinforage.forage.android.pos

import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.fixtures.givenContentId
import com.joinforage.forage.android.fixtures.givenEncryptionKey
import com.joinforage.forage.android.fixtures.givenPaymentAndRefundRef
import com.joinforage.forage.android.fixtures.givenPaymentMethodRef
import com.joinforage.forage.android.fixtures.givenPaymentRef
import com.joinforage.forage.android.fixtures.returnsEncryptionKeySuccessfully
import com.joinforage.forage.android.fixtures.returnsFailed
import com.joinforage.forage.android.fixtures.returnsFailedPayment
import com.joinforage.forage.android.fixtures.returnsFailedPaymentMethod
import com.joinforage.forage.android.fixtures.returnsFailedRefund
import com.joinforage.forage.android.fixtures.returnsMessageCompletedSuccessfully
import com.joinforage.forage.android.fixtures.returnsPayment
import com.joinforage.forage.android.fixtures.returnsPaymentMethod
import com.joinforage.forage.android.fixtures.returnsUnauthorizedEncryptionKey
import com.joinforage.forage.android.mock.MOCK_VAULT_REFUND_RESPONSE
import com.joinforage.forage.android.mock.MockServiceFactory
import com.joinforage.forage.android.mock.mockSuccessfulPosRefund
import com.joinforage.forage.android.network.data.MockVaultSubmitter
import com.joinforage.forage.android.network.data.TestPinCollector
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

    private lateinit var mockServiceFactory: MockServiceFactory
    private val mockVaultSubmitter = MockVaultSubmitter()
    private val expectedData = MockServiceFactory.ExpectedData
    private lateinit var mockForagePinEditText: ForagePINEditText

    @Before
    override fun setup() {
        super.setup()

        mockForagePinEditText = mock(ForagePINEditText::class.java)
        val logger = Log.getSilentInstance()
        mockServiceFactory = MockServiceFactory(
            mockVaultSubmitter = mockVaultSubmitter,
            mockPinCollector = TestPinCollector(),
            logger = logger,
            server = server
        )
        repository = mockServiceFactory.createRefundPaymentRepository(mockForagePinEditText)
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
        mockVaultSubmitter.setSubmitResponse(
            params = MockVaultSubmitter.RequestContainer(
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

        val expectedMessage = "Cannot find payment."
        val expectedForageCode = "not_found"
        val expectedStatusCode = 404

        val response = executeRefundPayment()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val firstError = (response as ForageApiResponse.Failure).errors.first()

        assertThat(firstError.message).isEqualTo(expectedMessage)
        assertThat(firstError.code).isEqualTo(expectedForageCode)
        assertThat(firstError.httpStatusCode).isEqualTo(expectedStatusCode)
    }

    @Test
    fun `it should return a failure when the get payment method returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsFailedPaymentMethod()

        val expectedMessage = "EBT Card could not be found"
        val expectedForageCode = "not_found"
        val expectedStatusCode = 404

        val response = executeRefundPayment()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val firstError = (response as ForageApiResponse.Failure).errors.first()

        assertThat(firstError.message).isEqualTo(expectedMessage)
        assertThat(firstError.code).isEqualTo(expectedForageCode)
        assertThat(firstError.httpStatusCode).isEqualTo(expectedStatusCode)
    }

    @Test
    fun `it should fail if getting the refund fails`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        server.givenContentId(expectedData.contentId)
            .returnsMessageCompletedSuccessfully()
        server.givenPaymentAndRefundRef().returnsFailedRefund()

        setVaultResponse(ForageApiResponse.Success(MOCK_VAULT_REFUND_RESPONSE))

        val expectedMessage = "Refund with ref refund123 does not exist for current Merchant with FNS 1234567."
        val expectedCode = "resource_not_found"
        val expectedStatusCode = 404

        val response = executeRefundPayment()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val firstError = (response as ForageApiResponse.Failure).errors.first()
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
        val firstError = (response as ForageApiResponse.Failure).errors.first()
        assertThat(firstError.message).isEqualTo(expectedMessage)
        assertThat(firstError.code).isEqualTo(expectedForageCode)
        assertThat(firstError.httpStatusCode).isEqualTo(expectedStatusCode)
    }

    @Test
    fun `it should fail when polling receives a failed SQS message`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsPaymentMethod()

        setVaultResponse(ForageApiResponse.Success(MOCK_VAULT_REFUND_RESPONSE))

        server.givenContentId(expectedData.contentId)
            .returnsFailed()

        val response = executeRefundPayment()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val firstError = (response as ForageApiResponse.Failure).errors.first()

        assertThat(firstError.httpStatusCode).isEqualTo(504)
        assertThat(firstError.code).isEqualTo("ebt_error_91")
        assertThat(firstError.message).contains("Authorizer not available (time-out) - Host Not Available")
    }

    @Test
    fun `it should succeed`() = runTest {
        mockSuccessfulPosRefund(mockVaultSubmitter = mockVaultSubmitter, server = server)

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
}

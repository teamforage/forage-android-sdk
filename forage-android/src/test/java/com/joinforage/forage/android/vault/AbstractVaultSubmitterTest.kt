package com.joinforage.forage.android.vault

import android.content.Context
import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeys
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.core.services.vault.VaultProxyRequest
import com.joinforage.forage.android.core.services.vault.VaultSubmitterParams
import com.joinforage.forage.android.core.ui.element.state.INITIAL_PIN_ELEMENT_STATE
import com.joinforage.forage.android.core.ui.element.state.pin.PinEditTextState
import com.joinforage.forage.android.ecom.ui.element.ForagePINEditText
import com.joinforage.forage.android.mock.MockLogger
import com.joinforage.forage.android.mock.MockServiceFactory
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class AbstractVaultSubmitterTest : MockServerSuite() {
    private lateinit var mockLogger: MockLogger
    private lateinit var mockForagePinEditText: ForagePINEditText
    private lateinit var mockContext: Context
    private lateinit var abstractVaultSubmitter: AbstractVaultSubmitter

    companion object {
        private val mockEncryptionKeys = EncryptionKeys("vgs-alias", "bt-alias")
        private val mockPaymentMethod = MockServiceFactory.ExpectedData.mockPaymentMethod
        private val mockVaultParams = VaultSubmitterParams(
            encryptionKeys = mockEncryptionKeys,
            idempotencyKey = "mock-idempotency-key",
            merchantId = "1234567",
            path = "/api/payments/abcdefg123/capture/",
            paymentMethod = mockPaymentMethod,
            userAction = UserAction.CAPTURE,
            sessionToken = "local_mock-session-token"
        )
    }

    @Before
    fun setUp() {
        super.setup()

        mockLogger = MockLogger()
        mockForagePinEditText = mock(ForagePINEditText::class.java)
        mockContext = mock(Context::class.java)

        val state = PinEditTextState.forEmptyInput(FocusState)
        state.isComplete = true
        `when`(mockForagePinEditText.getElementState()).thenReturn(state)

        abstractVaultSubmitter = ConcreteVaultSubmitter(
            context = mockContext,
            foragePinEditText = mockForagePinEditText,
            logger = mockLogger
        )
    }

    @Test
    fun `submit with invalid PIN returns IncompletePinError`() = runTest {
        val state = INITIAL_PIN_ELEMENT_STATE.copy(isComplete = false)

        `when`(mockForagePinEditText.getElementState()).thenReturn(state)
        val response = abstractVaultSubmitter.submit(mockVaultParams)

        val forageError = (response as ForageApiResponse.Failure).errors.first()
        assertEquals(forageError.code, "user_error")
        assertEquals(forageError.httpStatusCode, 400)
    }

    @Test
    fun `submit with successful vault proxy response returns Success`() = runTest {
        val concreteSubmitter = object : ConcreteVaultSubmitter(
            context = mockContext,
            foragePinEditText = mockForagePinEditText,
            logger = mockLogger
        ) {
            override suspend fun submitProxyRequest(
                vaultProxyRequest: VaultProxyRequest
            ): ForageApiResponse<String> {
                return ForageApiResponse.Success("success")
            }
        }

        val response = concreteSubmitter.submit(mockVaultParams)

        assertTrue(response is ForageApiResponse.Success)
    }

    @Test
    fun `submit with failed proxy response returns Failure`() = runTest {
        val concreteVaultSubmitter = object : ConcreteVaultSubmitter(
            context = mockContext,
            foragePinEditText = mockForagePinEditText,
            logger = mockLogger
        ) {
            override suspend fun submitProxyRequest(
                vaultProxyRequest: VaultProxyRequest
            ): ForageApiResponse<String> {
                return UnknownErrorApiResponse
            }
        }

        val response = concreteVaultSubmitter.submit(mockVaultParams)

        assertTrue(response is ForageApiResponse.Failure)
    }

    @Test
    fun `submit with missing vault token returns UnknownErrorApiResponse`() = runTest {
        val concreteSubmitter = object : ConcreteVaultSubmitter(
            context = mockContext,
            foragePinEditText = mockForagePinEditText,
            logger = mockLogger
        ) {
            // Mock missing token
            override fun getVaultToken(paymentMethod: PaymentMethod): String? {
                return null
            }
        }

        val response = concreteSubmitter.submit(mockVaultParams)

        val forageError = (response as ForageApiResponse.Failure).errors.first()
        assertEquals("unknown_server_error", forageError.code)
        assertEquals(500, forageError.httpStatusCode)
        assertEquals("Unknown Server Error", forageError.message)
    }

    @Test
    fun `calls clearText after submitting`() = runTest {
        abstractVaultSubmitter.submit(mockVaultParams)

        verify(mockForagePinEditText, times(1)).clearText()
    }

    @Test
    fun `grabs the correct vault token`() = runTest {
        val basisTheorySubmitter = object : ConcreteVaultSubmitter(
            context = mockContext,
            foragePinEditText = mockForagePinEditText,
            logger = mockLogger
        ) {
            override fun getVaultToken(paymentMethod: PaymentMethod): String? {
                return pickVaultTokenByIndex(paymentMethod, 1)
            }
        }

        val vgsSubmitter = object : ConcreteVaultSubmitter(
            context = mockContext,
            foragePinEditText = mockForagePinEditText,
            logger = mockLogger
        ) {
            override fun getVaultToken(paymentMethod: PaymentMethod): String? {
                return pickVaultTokenByIndex(paymentMethod, 0)
            }
        }

        val basisTheoryVaultToken = basisTheorySubmitter.getVaultToken(mockVaultParams.paymentMethod)
        val vgsVaultToken = vgsSubmitter.getVaultToken(mockVaultParams.paymentMethod)

        assertEquals("tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7", vgsVaultToken)
        assertEquals("basis-theory-token", basisTheoryVaultToken)
    }

    @Test
    fun `success metrics event is reported`() = runTest {
        val concreteVaultSubmitter = object : ConcreteVaultSubmitter(
            context = mockContext,
            foragePinEditText = mockForagePinEditText,
            logger = mockLogger
        ) {
            override suspend fun submitProxyRequest(
                vaultProxyRequest: VaultProxyRequest
            ): ForageApiResponse<String> {
                return ForageApiResponse.Success("success")
            }
        }

        concreteVaultSubmitter.submit(mockVaultParams)

        assertThat(mockLogger.infoLogs).anyMatch { logEntry ->
            logEntry.getMessage().contains("[Metrics] Received response from vgs proxy")
        }
        val attributes = mockLogger.getMetricsLog().getAttributes()
        assertThat(attributes.getValue("response_time_ms").toString().toDouble()).isGreaterThan(0.0)
        assertThat(attributes.getValue("vault_type").toString()).isEqualTo("vgs")
        assertThat(attributes.getValue("action").toString()).isEqualTo("capture")
        assertThat(attributes.getValue("event_name").toString()).isEqualTo("vault_response")
        assertThat(attributes.getValue("log_type").toString()).isEqualTo("metric")
        assertThat(attributes.getValue("forage_error_code").toString()).isEqualTo("unknown")
        assertThat(attributes.getValue("path").toString()).isEqualTo("/api/payments/abcdefg123/capture/")
    }

    @Test
    fun `failure metrics event is reported`() = runTest {
        val concreteVaultSubmitter = object : ConcreteVaultSubmitter(
            context = mockContext,
            foragePinEditText = mockForagePinEditText,
            logger = mockLogger
        ) {
            override suspend fun submitProxyRequest(
                vaultProxyRequest: VaultProxyRequest
            ): ForageApiResponse<String> {
                return UnknownErrorApiResponse
            }
        }

        concreteVaultSubmitter.submit(
            VaultSubmitterParams(
                encryptionKeys = mockEncryptionKeys,
                idempotencyKey = "mock-idempotency-key",
                merchantId = "1234567",
                path = "/api/payment_methods/abcdefg123/balance/",
                paymentMethod = mockPaymentMethod,
                // we cover BALANCE here instead of CAPTURE
                userAction = UserAction.BALANCE,
                sessionToken = "local_mock-session-token"
            )
        )

        val metricsLog = mockLogger.getMetricsLog()

        assertThat(mockLogger.infoLogs).anyMatch { logEntry ->
            logEntry.getMessage().contains("[Metrics] Received response from vgs proxy")
        }
        val attributes = metricsLog.getAttributes()
        assertThat(attributes.getValue("response_time_ms").toString().toDouble()).isGreaterThan(0.0)
        assertThat(attributes.getValue("vault_type").toString()).isEqualTo("vgs")
        assertThat(attributes.getValue("action").toString()).isEqualTo("balance")
        assertThat(attributes.getValue("event_name").toString()).isEqualTo("vault_response")
        assertThat(attributes.getValue("path").toString()).isEqualTo("/api/payment_methods/abcdefg123/balance/")
        assertThat(attributes.getValue("log_type").toString()).isEqualTo("metric")
        assertThat(attributes.getValue("forage_error_code").toString()).isEqualTo("unknown_server_error")
    }
}

internal open class ConcreteVaultSubmitter(
    context: Context,
    foragePinEditText: ForagePINEditText,
    logger: Log
) : AbstractVaultSubmitter(
    context = context,
    foragePinEditText = foragePinEditText,
    logger = logger
) {
    override val vaultType: VaultType = VaultType.VGS_VAULT_TYPE
    override fun parseEncryptionKey(encryptionKeys: EncryptionKeys): String {
        return "mock-encryption-key-alias"
    }

    override suspend fun submitProxyRequest(
        vaultProxyRequest: VaultProxyRequest
    ): ForageApiResponse<String> {
        return ForageApiResponse.Success("success")
    }

    override fun getVaultToken(paymentMethod: PaymentMethod): String? {
        return "mock-vault-token"
    }
}

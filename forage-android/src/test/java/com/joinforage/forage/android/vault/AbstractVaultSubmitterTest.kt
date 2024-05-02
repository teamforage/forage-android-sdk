package com.joinforage.forage.android.vault

import android.content.Context
import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.ui.element.state.INITIAL_PIN_ELEMENT_STATE
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.mock.MockLogger
import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeys
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtCard
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.ecom.ui.ForagePINEditText
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
    private lateinit var abstractVaultSubmitter: AbstractVaultSubmitter<Any>

    companion object {
        private val mockEncryptionKeys = EncryptionKeys("vgs-alias", "bt-alias")
        private val mockPaymentMethod = PaymentMethod(
            ref = "1f148fe399",
            type = "ebt",
            balance = null,
            card = EbtCard(
                last4 = "7845",
                token = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7,basis-theory-token",
                fingerprint = "fingerprint"
            ),
            customerId = "test-android-customer-id",
            reusable = true
        )
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

        val state = INITIAL_PIN_ELEMENT_STATE.copy(isComplete = true)
        `when`(mockForagePinEditText.getElementState()).thenReturn(state)

        abstractVaultSubmitter = ConcreteVaultSubmitter(
            context = mockContext,
            foragePinEditText = mockForagePinEditText,
            logger = mockLogger,
            vaultType = VaultType.BT_VAULT_TYPE
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
            logger = mockLogger,
            vaultType = VaultType.VGS_VAULT_TYPE
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
            logger = mockLogger,
            vaultType = VaultType.VGS_VAULT_TYPE
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
            logger = mockLogger,
            vaultType = VaultType.BT_VAULT_TYPE
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
            logger = mockLogger,
            vaultType = VaultType.BT_VAULT_TYPE
        ) {
            override fun getVaultToken(paymentMethod: PaymentMethod): String? {
                return pickVaultTokenByIndex(paymentMethod, 1)
            }
        }

        val vgsSubmitter = object : ConcreteVaultSubmitter(
            context = mockContext,
            foragePinEditText = mockForagePinEditText,
            logger = mockLogger,
            vaultType = VaultType.VGS_VAULT_TYPE
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
            logger = mockLogger,
            vaultType = VaultType.VGS_VAULT_TYPE
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
            logger = mockLogger,
            vaultType = VaultType.BT_VAULT_TYPE
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
            logEntry.getMessage().contains("[Metrics] Received response from basis_theory proxy")
        }
        val attributes = metricsLog.getAttributes()
        assertThat(attributes.getValue("response_time_ms").toString().toDouble()).isGreaterThan(0.0)
        assertThat(attributes.getValue("vault_type").toString()).isEqualTo("basis_theory")
        assertThat(attributes.getValue("action").toString()).isEqualTo("balance")
        assertThat(attributes.getValue("event_name").toString()).isEqualTo("vault_response")
        assertThat(attributes.getValue("path").toString()).isEqualTo("/api/payment_methods/abcdefg123/balance/")
        assertThat(attributes.getValue("log_type").toString()).isEqualTo("metric")
        assertThat(attributes.getValue("forage_error_code").toString()).isEqualTo("unknown_server_error")
    }

    @Test
    fun `creates the correct vault submitter`() = runTest {
        val mockBasisTheoryPinEditText = mock(ForagePINEditText::class.java)
        `when`(mockBasisTheoryPinEditText.getVaultType()).thenReturn(VaultType.BT_VAULT_TYPE)
        `when`(mockBasisTheoryPinEditText.context).thenReturn(mockContext)

        val mockVgsPinEditText = mock(ForagePINEditText::class.java)
        `when`(mockVgsPinEditText.getVaultType()).thenReturn(VaultType.VGS_VAULT_TYPE)
        `when`(mockVgsPinEditText.context).thenReturn(mockContext)

        val btVaultSubmitter = AbstractVaultSubmitter.create(mockBasisTheoryPinEditText, mockLogger)
        val vgsVaultSubmitter = AbstractVaultSubmitter.create(mockVgsPinEditText, mockLogger)

        assertTrue(vgsVaultSubmitter is VgsPinSubmitter)
        assertTrue(btVaultSubmitter is BasisTheoryPinSubmitter)
    }
}

internal open class ConcreteVaultSubmitter(
    context: Context,
    foragePinEditText: ForagePINEditText,
    logger: Log,
    vaultType: VaultType = VaultType.VGS_VAULT_TYPE
) : AbstractVaultSubmitter<Any>(
    context = context,
    foragePinEditText = foragePinEditText,
    logger = logger,
    vaultType = vaultType
) {
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

    override fun parseVaultErrorMessage(vaultResponse: Any): String {
        return "Mock Forage Vault Error"
    }

    override fun toForageSuccessOrNull(vaultResponse: Any): ForageApiResponse.Success<String>? {
        return null
    }

    override fun toForageErrorOrNull(vaultResponse: Any): ForageApiResponse.Failure? {
        return null
    }

    override fun toVaultErrorOrNull(vaultResponse: Any): ForageApiResponse.Failure? {
        return null
    }
}

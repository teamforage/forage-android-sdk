package com.joinforage.forage.android.vault

import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.core.services.vault.SecurePinCollector
import com.joinforage.forage.android.core.services.vault.VaultProxyRequest
import com.joinforage.forage.android.core.services.vault.VaultSubmitterParams
import com.joinforage.forage.android.mock.MockLogger
import com.joinforage.forage.android.mock.MockServiceFactory
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AbstractVaultSubmitterTest : MockServerSuite() {
    private lateinit var mockLogger: MockLogger
    private lateinit var abstractVaultSubmitter: AbstractVaultSubmitter
    private val mockCollector = object : SecurePinCollector {
        override fun clearText() {}
        override fun isComplete(): Boolean = true
    }

    companion object {
        private val mockPaymentMethod = MockServiceFactory.ExpectedData.mockPaymentMethod
        private val mockVaultParams = VaultSubmitterParams(
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
        abstractVaultSubmitter = ConcreteVaultSubmitter(
            collector = mockCollector,
            logger = mockLogger
        )
    }

    @Test
    fun `submit with invalid PIN returns IncompletePinError`() = runTest {
        val incompleteCollector = object : SecurePinCollector {
            override fun clearText() {}
            override fun isComplete(): Boolean = false
        }

        val abstractVaultSubmitter = ConcreteVaultSubmitter(
            collector = incompleteCollector,
            logger = mockLogger
        )

        val response = abstractVaultSubmitter.submit(mockVaultParams)

        val forageError = (response as ForageApiResponse.Failure).errors.first()
        assertEquals(forageError.code, "user_error")
        assertEquals(forageError.httpStatusCode, 400)
    }

    @Test
    fun `submit with successful vault proxy response returns Success`() = runTest {
        val concreteSubmitter = object : ConcreteVaultSubmitter(
            collector = mockCollector,
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
            collector = mockCollector,
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
            collector = mockCollector,
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
    fun `calls clearText after submitting on success`() = runTest {
        var numTimesClearTextCalled = 0
        val clearTextSpyCollector = object : SecurePinCollector {
            override fun clearText() {
                numTimesClearTextCalled += 1
            }
            override fun isComplete(): Boolean = true
        }

        val successVaultSubmitter = object : ConcreteVaultSubmitter(
            collector = clearTextSpyCollector,
            logger = mockLogger
        ) {
            override suspend fun submitProxyRequest(
                vaultProxyRequest: VaultProxyRequest
            ): ForageApiResponse<String> {
                return ForageApiResponse.Success("success")
            }
        }
        successVaultSubmitter.submit(mockVaultParams)
        assertEquals(1, numTimesClearTextCalled)
    }

    @Test
    fun `calls clearText after submitting on failure`() = runTest {
        var numTimesClearTextCalled = 0
        val clearTextSpyCollector = object : SecurePinCollector {
            override fun clearText() {
                numTimesClearTextCalled += 1
            }
            override fun isComplete(): Boolean = true
        }

        val failedVaultSubmitter = object : ConcreteVaultSubmitter(
            collector = clearTextSpyCollector,
            logger = mockLogger
        ) {
            override suspend fun submitProxyRequest(
                vaultProxyRequest: VaultProxyRequest
            ): ForageApiResponse<String> {
                return UnknownErrorApiResponse
            }
        }
        failedVaultSubmitter.submit(mockVaultParams)
        assertEquals(1, numTimesClearTextCalled)
    }

    @Test
    fun `grabs the correct vault token`() = runTest {
        val forageSubmitter = object : ConcreteVaultSubmitter(
            collector = mockCollector,
            logger = mockLogger
        ) {
            override fun getVaultToken(paymentMethod: PaymentMethod): String? {
                return pickVaultTokenByIndex(paymentMethod, 2)
            }
        }

        val forageVaultToken = forageSubmitter.getVaultToken(mockVaultParams.paymentMethod)

        assertEquals("forage-token", forageVaultToken)
    }

    @Test
    fun `success metrics event is reported`() = runTest {
        val concreteVaultSubmitter = object : ConcreteVaultSubmitter(
            collector = mockCollector,
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
            logEntry.getMessage().contains("[Metrics] Received response from forage proxy")
        }
        val attributes = mockLogger.getMetricsLog().getAttributes()
        assertThat(attributes.getValue("response_time_ms").toString().toDouble()).isGreaterThan(0.0)
        assertThat(attributes.getValue("vault_type").toString()).isEqualTo("forage")
        assertThat(attributes.getValue("action").toString()).isEqualTo("capture")
        assertThat(attributes.getValue("event_name").toString()).isEqualTo("vault_response")
        assertThat(attributes.getValue("log_type").toString()).isEqualTo("metric")
        assertThat(attributes.getValue("forage_error_code").toString()).isEqualTo("unknown")
        assertThat(attributes.getValue("path").toString()).isEqualTo("/api/payments/abcdefg123/capture/")
    }

    @Test
    fun `failure metrics event is reported`() = runTest {
        val concreteVaultSubmitter = object : ConcreteVaultSubmitter(
            collector = mockCollector,
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
            logEntry.getMessage().contains("[Metrics] Received response from forage proxy")
        }
        val attributes = metricsLog.getAttributes()
        assertThat(attributes.getValue("response_time_ms").toString().toDouble()).isGreaterThan(0.0)
        assertThat(attributes.getValue("vault_type").toString()).isEqualTo("forage")
        assertThat(attributes.getValue("action").toString()).isEqualTo("balance")
        assertThat(attributes.getValue("event_name").toString()).isEqualTo("vault_response")
        assertThat(attributes.getValue("path").toString()).isEqualTo("/api/payment_methods/abcdefg123/balance/")
        assertThat(attributes.getValue("log_type").toString()).isEqualTo("metric")
        assertThat(attributes.getValue("forage_error_code").toString()).isEqualTo("unknown_server_error")
    }
}

internal open class ConcreteVaultSubmitter(
    collector: SecurePinCollector,
    logger: Log
) : AbstractVaultSubmitter(
    collector = collector,
    logger = logger
) {
    override val vaultType: VaultType = VaultType.FORAGE_VAULT_TYPE

    override suspend fun submitProxyRequest(
        vaultProxyRequest: VaultProxyRequest
    ): ForageApiResponse<String> {
        return ForageApiResponse.Success("success")
    }

    override fun getVaultToken(paymentMethod: PaymentMethod): String? {
        return "mock-vault-token"
    }
}

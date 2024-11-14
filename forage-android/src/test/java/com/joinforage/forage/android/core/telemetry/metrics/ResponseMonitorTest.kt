package com.joinforage.forage.android.core.telemetry.metrics

import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.services.telemetry.CustomerPerceivedResponseMonitor
import com.joinforage.forage.android.core.services.telemetry.EventName
import com.joinforage.forage.android.core.services.telemetry.EventOutcome
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.telemetry.LogType
import com.joinforage.forage.android.core.services.telemetry.MetricsConstants
import com.joinforage.forage.android.core.services.telemetry.ResponseMonitor
import com.joinforage.forage.android.core.services.telemetry.UnknownForageErrorCode
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.telemetry.VaultProxyResponseMonitor
import com.joinforage.forage.android.mock.MockLogger
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

internal class TestResponseMonitor(metricsLogger: Log?) : ResponseMonitor<TestResponseMonitor>(metricsLogger) {
    override fun logWithResponseAttributes(
        metricsLogger: Log?,
        responseAttributes: Map<String, Any>
    ) {
        metricsLogger?.i("Successfully logged!", responseAttributes)
    }
}

@RunWith(RobolectricTestRunner::class)
class ResponseMonitorTest {

    @Test
    fun `Test response monitor should calculate duration`() {
        val mockLogger = MockLogger()
        val testResponseMonitor = TestResponseMonitor(mockLogger)
        // Simulate some network delay
        Thread.sleep(100)
        testResponseMonitor.logResult()
        Assertions.assertThat(mockLogger.errorLogs.count()).isEqualTo(0)
        Assertions.assertThat(mockLogger.infoLogs.count()).isEqualTo(1)
        val infoLog = mockLogger.infoLogs[0]
        Assertions.assertThat(infoLog.getMessage()).isEqualTo("Successfully logged!")
        val waitTime = infoLog.getAttributes()[MetricsConstants.RESPONSE_TIME_MS] as? Double
        Assertions.assertThat(waitTime).isGreaterThanOrEqualTo(100.0)
    }

    @Test
    fun `Vault proxy monitor should log error if path is not set`() {
        val mockLogger = MockLogger()
        val vaultProxyResponseMonitor = VaultProxyResponseMonitor(userAction = UserAction.CAPTURE, mockLogger)
        vaultProxyResponseMonitor.setMethod("POST").setHttpStatusCode(200).logResult()
        Assertions.assertThat(mockLogger.errorLogs.count()).isEqualTo(1)
        Assertions.assertThat(mockLogger.infoLogs.count()).isEqualTo(0)
        Assertions.assertThat(mockLogger.errorLogs[0].getMessage()).isEqualTo("[Metrics] Incomplete or missing response attributes. Could not log metric.")
    }

    @Test
    fun `Vault proxy monitor should log error if method is not set`() {
        val mockLogger = MockLogger()
        val vaultProxyResponseMonitor = VaultProxyResponseMonitor(userAction = UserAction.CAPTURE, mockLogger)
        vaultProxyResponseMonitor.setPath("this/is/test/path/").setHttpStatusCode(200).logResult()
        Assertions.assertThat(mockLogger.errorLogs.count()).isEqualTo(1)
        Assertions.assertThat(mockLogger.infoLogs.count()).isEqualTo(0)
        Assertions.assertThat(mockLogger.errorLogs[0].getMessage()).isEqualTo("[Metrics] Incomplete or missing response attributes. Could not log metric.")
    }

    @Test
    fun `Vault proxy monitor should log error if status code is not set`() {
        val mockLogger = MockLogger()
        val vaultProxyResponseMonitor = VaultProxyResponseMonitor(userAction = UserAction.CAPTURE, mockLogger)
        vaultProxyResponseMonitor.setPath("this/is/test/path/").setMethod("POST").logResult()
        Assertions.assertThat(mockLogger.errorLogs.count()).isEqualTo(1)
        Assertions.assertThat(mockLogger.infoLogs.count()).isEqualTo(0)
        Assertions.assertThat(mockLogger.errorLogs[0].getMessage()).isEqualTo("[Metrics] Incomplete or missing response attributes. Could not log metric.")
    }

    @Test
    fun `Validate the attributes of a successful vault proxy log`() {
        val mockLogger = MockLogger()
        val vaultType = VaultType.FORAGE_VAULT_TYPE
        val userAction = UserAction.CAPTURE
        val vaultProxyResponseMonitor = VaultProxyResponseMonitor(userAction = userAction, mockLogger)
        val path = "this/is/test/path/"
        val method = "POST"
        val statusCode = 200
        vaultProxyResponseMonitor.setPath(path).setMethod(method).setHttpStatusCode(statusCode).logResult()

        Assertions.assertThat(mockLogger.errorLogs.count()).isEqualTo(0)
        Assertions.assertThat(mockLogger.infoLogs.count()).isEqualTo(1)

        val msg = mockLogger.infoLogs[0].getMessage()

        Assertions.assertThat(msg).isEqualTo("[Metrics] Received response from $vaultType proxy")

        val attributes = mockLogger.infoLogs[0].getAttributes()
        val loggedPath = attributes[MetricsConstants.PATH]
        val loggedMethod = attributes[MetricsConstants.METHOD]
        val loggedStatusCode = attributes[MetricsConstants.HTTP_STATUS]
        val loggedVaultType = attributes[MetricsConstants.VAULT_TYPE]
        val loggedVaultAction = attributes[MetricsConstants.ACTION]
        val loggedResponseType = attributes[MetricsConstants.EVENT_NAME]
        val loggedLogType = attributes[MetricsConstants.LOG_TYPE]

        Assertions.assertThat(loggedPath).isEqualTo(path)
        Assertions.assertThat(loggedMethod).isEqualTo(method)
        Assertions.assertThat(loggedStatusCode).isEqualTo(statusCode)
        Assertions.assertThat(loggedVaultType).isEqualTo(vaultType)
        Assertions.assertThat(loggedVaultAction).isEqualTo(userAction)
        Assertions.assertThat(loggedResponseType).isEqualTo(EventName.VAULT_RESPONSE)
        Assertions.assertThat(loggedLogType).isEqualTo(LogType.METRIC)
    }

    @Test
    fun `Customer perceived monitor should log error if outcome type is not set`() {
        val mockLogger = MockLogger()
        val customerPerceivedResponseMonitor = CustomerPerceivedResponseMonitor(userAction = UserAction.CAPTURE, mockLogger)
        customerPerceivedResponseMonitor.logResult()
        Assertions.assertThat(mockLogger.errorLogs.count()).isEqualTo(1)
        Assertions.assertThat(mockLogger.infoLogs.count()).isEqualTo(0)
        Assertions.assertThat(mockLogger.errorLogs[0].getMessage()).isEqualTo("[Metrics] Incomplete or missing response attributes. Could not log metric.")
    }

    @Test
    fun `Validate the attributes of a successful customer perceived response time log`() {
        val mockLogger = MockLogger()
        val vaultType = VaultType.FORAGE_VAULT_TYPE
        val userAction = UserAction.CAPTURE
        val roundTripResponseMonitor = CustomerPerceivedResponseMonitor(userAction = userAction, mockLogger)
        roundTripResponseMonitor.setEventOutcome(EventOutcome.SUCCESS).setHttpStatusCode(200).logResult()

        Assertions.assertThat(mockLogger.errorLogs.count()).isEqualTo(0)
        Assertions.assertThat(mockLogger.infoLogs.count()).isEqualTo(1)

        val msg = mockLogger.infoLogs[0].getMessage()

        Assertions.assertThat(msg).isEqualTo("[Metrics] Customer perceived response time for $vaultType has been collected")

        val attributes = mockLogger.infoLogs[0].getAttributes()
        val loggedVaultType = attributes[MetricsConstants.VAULT_TYPE]
        val loggedVaultAction = attributes[MetricsConstants.ACTION]
        val loggedEventName = attributes[MetricsConstants.EVENT_NAME]
        val loggedOutcomeType = attributes[MetricsConstants.EVENT_OUTCOME]
        val loggedForageErrorCode = attributes[MetricsConstants.FORAGE_ERROR_CODE]
        val loggedLogType = attributes[MetricsConstants.LOG_TYPE]
        val loggedHttpStatus = attributes[MetricsConstants.HTTP_STATUS]

        Assertions.assertThat(loggedVaultType).isEqualTo(vaultType)
        Assertions.assertThat(loggedVaultAction).isEqualTo(userAction)
        Assertions.assertThat(loggedEventName).isEqualTo(EventName.CUSTOMER_PERCEIVED_RESPONSE)
        Assertions.assertThat(loggedOutcomeType).isEqualTo(EventOutcome.SUCCESS)
        Assertions.assertThat(loggedForageErrorCode).isEqualTo(UnknownForageErrorCode.UNKNOWN)
        Assertions.assertThat(loggedLogType).isEqualTo(LogType.METRIC)
        Assertions.assertThat(loggedHttpStatus).isEqualTo(200)
    }
}

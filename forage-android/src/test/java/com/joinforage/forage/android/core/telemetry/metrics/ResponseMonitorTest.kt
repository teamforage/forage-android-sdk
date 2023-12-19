package com.joinforage.forage.android.core.telemetry.metrics

import android.content.Context
import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.core.telemetry.CustomerPerceivedResponseMonitor
import com.joinforage.forage.android.core.telemetry.EventName
import com.joinforage.forage.android.core.telemetry.EventOutcome
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.core.telemetry.LogType
import com.joinforage.forage.android.core.telemetry.MetricsConstants
import com.joinforage.forage.android.core.telemetry.ResponseMonitor
import com.joinforage.forage.android.core.telemetry.UnknownForageErrorCode
import com.joinforage.forage.android.core.telemetry.UserAction
import com.joinforage.forage.android.core.telemetry.VaultProxyResponseMonitor
import com.joinforage.forage.android.ui.ForageConfig
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

internal class LogEntry(message: String, attributes: Map<String, Any?>) {
    private var message = ""
    private var attributes = mapOf<String, Any?>()

    init {
        this.message = message
        this.attributes = attributes
    }

    fun getMessage(): String {
        return message
    }

    fun getAttributes(): Map<String, Any?> {
        return attributes
    }
}

internal class MockLogger : Log {
    val infoLogs: MutableList<LogEntry> = mutableListOf()
    val errorLogs: MutableList<LogEntry> = mutableListOf()

    override fun initializeDD(context: Context, forageConfig: ForageConfig) {
        return
    }

    override fun d(msg: String, attributes: Map<String, Any?>) {
        return
    }

    override fun i(msg: String, attributes: Map<String, Any?>) {
        infoLogs.add(LogEntry(msg, attributes))
    }

    override fun w(msg: String, attributes: Map<String, Any?>) {
        return
    }

    override fun e(msg: String, throwable: Throwable?, attributes: Map<String, Any?>) {
        errorLogs.add(LogEntry(msg, attributes))
    }

    override fun getTraceIdValue(): String {
        return ""
    }
}

internal class TestResponseMonitor(metricsLogger: Log?) : ResponseMonitor(metricsLogger) {
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
    fun `Test response monitor should log error if start isn't called`() {
        val mockLogger = MockLogger()
        val testResponseMonitor = TestResponseMonitor(mockLogger)
        testResponseMonitor.end()
        testResponseMonitor.logResult()
        Assertions.assertThat(mockLogger.errorLogs.count()).isEqualTo(1)
        Assertions.assertThat(mockLogger.infoLogs.count()).isEqualTo(0)
        Assertions.assertThat(mockLogger.errorLogs[0].getMessage()).isEqualTo("[Metrics] Missing startTime or endTime. Could not log metric.")
    }

    @Test
    fun `Test response monitor should log error if end isn't called`() {
        val mockLogger = MockLogger()
        val testResponseMonitor = TestResponseMonitor(mockLogger)
        testResponseMonitor.end()
        testResponseMonitor.logResult()
        Assertions.assertThat(mockLogger.errorLogs.count()).isEqualTo(1)
        Assertions.assertThat(mockLogger.infoLogs.count()).isEqualTo(0)
        Assertions.assertThat(mockLogger.errorLogs[0].getMessage()).isEqualTo("[Metrics] Missing startTime or endTime. Could not log metric.")
    }

    @Test
    fun `Test response monitor should calculate duration`() {
        val mockLogger = MockLogger()
        val testResponseMonitor = TestResponseMonitor(mockLogger)
        testResponseMonitor.start()
        // Simulate some network delay
        Thread.sleep(100)
        testResponseMonitor.end()
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
        val vaultProxyResponseMonitor = VaultProxyResponseMonitor(vault = VaultType.VGS_VAULT_TYPE, userAction = UserAction.CAPTURE, mockLogger)
        vaultProxyResponseMonitor.start()
        vaultProxyResponseMonitor.end()
        vaultProxyResponseMonitor.setMethod("POST").setHttpStatusCode(200).logResult()
        Assertions.assertThat(mockLogger.errorLogs.count()).isEqualTo(1)
        Assertions.assertThat(mockLogger.infoLogs.count()).isEqualTo(0)
        Assertions.assertThat(mockLogger.errorLogs[0].getMessage()).isEqualTo("[Metrics] Incomplete or missing response attributes. Could not log metric.")
    }

    @Test
    fun `Vault proxy monitor should log error if method is not set`() {
        val mockLogger = MockLogger()
        val vaultProxyResponseMonitor = VaultProxyResponseMonitor(vault = VaultType.VGS_VAULT_TYPE, userAction = UserAction.CAPTURE, mockLogger)
        vaultProxyResponseMonitor.start()
        vaultProxyResponseMonitor.end()
        vaultProxyResponseMonitor.setPath("this/is/test/path/").setHttpStatusCode(200).logResult()
        Assertions.assertThat(mockLogger.errorLogs.count()).isEqualTo(1)
        Assertions.assertThat(mockLogger.infoLogs.count()).isEqualTo(0)
        Assertions.assertThat(mockLogger.errorLogs[0].getMessage()).isEqualTo("[Metrics] Incomplete or missing response attributes. Could not log metric.")
    }

    @Test
    fun `Vault proxy monitor should log error if status code is not set`() {
        val mockLogger = MockLogger()
        val vaultProxyResponseMonitor = VaultProxyResponseMonitor(vault = VaultType.VGS_VAULT_TYPE, userAction = UserAction.CAPTURE, mockLogger)
        vaultProxyResponseMonitor.start()
        vaultProxyResponseMonitor.end()
        vaultProxyResponseMonitor.setPath("this/is/test/path/").setMethod("POST").logResult()
        Assertions.assertThat(mockLogger.errorLogs.count()).isEqualTo(1)
        Assertions.assertThat(mockLogger.infoLogs.count()).isEqualTo(0)
        Assertions.assertThat(mockLogger.errorLogs[0].getMessage()).isEqualTo("[Metrics] Incomplete or missing response attributes. Could not log metric.")
    }

    @Test
    fun `Validate the attributes of a successful vault proxy log`() {
        val mockLogger = MockLogger()
        val vaultType = VaultType.VGS_VAULT_TYPE
        val userAction = UserAction.CAPTURE
        val vaultProxyResponseMonitor = VaultProxyResponseMonitor(vault = vaultType, userAction = userAction, mockLogger)
        val path = "this/is/test/path/"
        val method = "POST"
        val statusCode = 200
        vaultProxyResponseMonitor.start()
        vaultProxyResponseMonitor.end()
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
        val customerPerceivedResponseMonitor = CustomerPerceivedResponseMonitor(vault = VaultType.VGS_VAULT_TYPE, userAction = UserAction.CAPTURE, mockLogger)
        customerPerceivedResponseMonitor.start()
        customerPerceivedResponseMonitor.end()
        customerPerceivedResponseMonitor.logResult()
        Assertions.assertThat(mockLogger.errorLogs.count()).isEqualTo(1)
        Assertions.assertThat(mockLogger.infoLogs.count()).isEqualTo(0)
        Assertions.assertThat(mockLogger.errorLogs[0].getMessage()).isEqualTo("[Metrics] Incomplete or missing response attributes. Could not log metric.")
    }

    @Test
    fun `Validate the attributes of a successful customer perceived response time log`() {
        val mockLogger = MockLogger()
        val vaultType = VaultType.VGS_VAULT_TYPE
        val userAction = UserAction.CAPTURE
        val roundTripResponseMonitor = CustomerPerceivedResponseMonitor(vault = vaultType, userAction = userAction, mockLogger)
        roundTripResponseMonitor.start()
        roundTripResponseMonitor.end()
        roundTripResponseMonitor.setEventOutcome(EventOutcome.SUCCESS).logResult()

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

        Assertions.assertThat(loggedVaultType).isEqualTo(vaultType)
        Assertions.assertThat(loggedVaultAction).isEqualTo(userAction)
        Assertions.assertThat(loggedEventName).isEqualTo(EventName.CUSTOMER_PERCEIVED_RESPONSE)
        Assertions.assertThat(loggedOutcomeType).isEqualTo(EventOutcome.SUCCESS)
        Assertions.assertThat(loggedForageErrorCode).isEqualTo(UnknownForageErrorCode.UNKNOWN)
        Assertions.assertThat(loggedLogType).isEqualTo(LogType.METRIC)
    }
}

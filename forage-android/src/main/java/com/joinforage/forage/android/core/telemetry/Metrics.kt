package com.joinforage.forage.android.core.telemetry
import com.joinforage.forage.android.VaultType

internal object MetricsConstants {
    const val PATH = "path"
    const val METHOD = "method"
    const val HTTP_STATUS = "http_status"
    const val RESPONSE_TIME_MS = "response_time_ms"
    const val ACTION = "action"
    const val VAULT_TYPE = "vault_type"
    const val EVENT_NAME = "event_name"
    const val EVENT_OUTCOME = "event_outcome"
    const val FORAGE_ERROR_CODE = "forage_error_code"
}

internal enum class UnknownForageErrorCode(val value: String) {
    UNKNOWN("unknown");

    override fun toString(): String {
        return value
    }
}

internal enum class UserAction(val value: String) {
    BALANCE("balance"),
    CAPTURE("capture");

    override fun toString(): String {
        return value
    }
}

internal enum class EventOutcome(val value: String) {
    SUCCESS("success"),
    FAILURE("failure");

    override fun toString(): String {
        return value
    }
}

internal enum class EventName(val value: String) {
    /*
    VAULT_RESPONSE refers to a response from the VGS or BT submit actions.
     */
    VAULT_RESPONSE("vault_response"),

    /*
    CUSTOMER_PERCEIVED_RESPONSE refers to the response from a balance or capture action. There are
    multiple chained requests that come from the client when executing a balance or capture action.
    Ex of a balance action:
    [GET] EncryptionKey -> [GET] PaymentMethod -> [POST] to VGS/BT -> [GET] Poll for Response ->
    [GET] PaymentMethod -> Return Balance
     */
    CUSTOMER_PERCEIVED_RESPONSE("customer_perceived_response");

    override fun toString(): String {
        return value
    }
}

internal interface PerformanceMeasurer {
    fun start()
    fun end()
    fun logResult()
}

internal interface NetworkMonitor : PerformanceMeasurer {
    fun setPath(path: String): NetworkMonitor
    fun setMethod(method: String): NetworkMonitor
    fun setHttpStatusCode(code: Int): NetworkMonitor
    fun setForageErrorCode(errorCode: String): NetworkMonitor
}

internal abstract class ResponseMonitor(metricsLogger: Log? = Log.getInstance()) : NetworkMonitor {
    private var startTime: Long? = null
    private var endTime: Long? = null

    private var logger: Log? = null
    private var responseAttributes: MutableMap<String, Any> = mutableMapOf()

    init {
        logger = metricsLogger
    }

    override fun start() {
        startTime = System.nanoTime()
    }

    override fun end() {
        endTime = System.nanoTime()
    }

    override fun setPath(path: String): NetworkMonitor {
        responseAttributes[MetricsConstants.PATH] = path
        return this
    }

    override fun setMethod(method: String): NetworkMonitor {
        responseAttributes[MetricsConstants.METHOD] = method
        return this
    }

    override fun setHttpStatusCode(code: Int): NetworkMonitor {
        responseAttributes[MetricsConstants.HTTP_STATUS] = code
        return this
    }

    override fun setForageErrorCode(errorCode: String): NetworkMonitor {
        responseAttributes[MetricsConstants.FORAGE_ERROR_CODE] = errorCode
        return this
    }

    override fun logResult() {
        val defaultVal = Long.MIN_VALUE
        val start = startTime ?: defaultVal
        val end = endTime ?: defaultVal

        if (start == defaultVal || end == defaultVal) {
            logger?.e("[Metrics] Missing startTime or endTime. Could not log metric.")
            return
        }

        responseAttributes[MetricsConstants.RESPONSE_TIME_MS] = calculateDuration(start, end)

        logWithResponseAttributes(metricsLogger = logger, responseAttributes = responseAttributes)
    }

    // Calculate the time in milliseconds between the start and end time
    private fun calculateDuration(startTime: Long, endTime: Long): Double {
        return (endTime - startTime).toDouble() / 1000000
    }

    abstract fun logWithResponseAttributes(metricsLogger: Log?, responseAttributes: Map<String, Any>)
}

/*
    VaultProxyResponseMonitor is used to track the response time from the VGS and BT submit
    functions. The timer begins when a balance or capture request is submitted to VGS/BT
    and ends when a response is received by the SDK.
     */
internal class VaultProxyResponseMonitor(vault: VaultType, userAction: UserAction, metricsLogger: Log?) : ResponseMonitor(metricsLogger) {
    private var vaultType: VaultType? = null
    private var userAction: UserAction? = null
    private var eventName: EventName = EventName.VAULT_RESPONSE

    init {
        this.vaultType = vault
        this.userAction = userAction
    }

    internal companion object {
        internal fun newMeasurement(vault: VaultType, userAction: UserAction, metricsLogger: Log?): VaultProxyResponseMonitor {
            return VaultProxyResponseMonitor(vault, userAction, metricsLogger)
        }
    }

    override fun logWithResponseAttributes(
        metricsLogger: Log?,
        responseAttributes: Map<String, Any>
    ) {
        val path = responseAttributes[MetricsConstants.PATH]
        val method = responseAttributes[MetricsConstants.METHOD]
        val httpStatus = responseAttributes[MetricsConstants.HTTP_STATUS]
        val responseTime = responseAttributes[MetricsConstants.RESPONSE_TIME_MS]
        val forageErrorCode = responseAttributes[MetricsConstants.FORAGE_ERROR_CODE]

        if (path == null || method == null || httpStatus == null || responseTime == null) {
            metricsLogger?.e("[Metrics] Incomplete or missing response attributes. Could not log metric.")
            return
        }

        val vaultType = vaultType
        val userAction = userAction

        val forageErrorCodeOrNull = forageErrorCode ?: UnknownForageErrorCode.UNKNOWN

        metricsLogger?.i(
            "[Metrics] Received response from $vaultType proxy",
            attributes = mapOf(
                MetricsConstants.PATH to path,
                MetricsConstants.METHOD to method,
                MetricsConstants.HTTP_STATUS to httpStatus,
                MetricsConstants.RESPONSE_TIME_MS to responseTime,
                MetricsConstants.VAULT_TYPE to vaultType,
                MetricsConstants.ACTION to userAction,
                MetricsConstants.EVENT_NAME to eventName,
                MetricsConstants.FORAGE_ERROR_CODE to forageErrorCodeOrNull
            )
        )
    }
}

/*
    CustomerPerceivedResponseMonitor is used to track the response time that a customer
    experiences while executing a balance or capture action. There are multiple chained requests
    that come from the client when executing a balance or capture action. The timer begins when the
    first HTTP request is sent from the SDK and ends when the the SDK returns information back to
    the user. Ex of a balance action:
    Timer Begins -> [GET] EncryptionKey -> [GET] PaymentMethod -> [POST] to VGS/BT ->
    [GET] Poll for Response -> [GET] PaymentMethod -> Timer Ends -> Return Balance
     */
internal class CustomerPerceivedResponseMonitor(vault: VaultType, userAction: UserAction, metricsLogger: Log?) : ResponseMonitor(metricsLogger) {
    private var vaultType: VaultType? = null
    private var userAction: UserAction? = null
    private var eventOutcome: EventOutcome? = null
    private var eventName: EventName = EventName.CUSTOMER_PERCEIVED_RESPONSE

    init {
        this.vaultType = vault
        this.userAction = userAction
    }

    internal companion object {
        internal fun newMeasurement(vault: VaultType, vaultAction: UserAction, metricsLogger: Log?): CustomerPerceivedResponseMonitor {
            return CustomerPerceivedResponseMonitor(vault, vaultAction, metricsLogger)
        }
    }

    fun setEventOutcome(eventOutcome: EventOutcome): CustomerPerceivedResponseMonitor {
        this.eventOutcome = eventOutcome
        return this
    }

    override fun logWithResponseAttributes(
        metricsLogger: Log?,
        responseAttributes: Map<String, Any>
    ) {
        val responseTime = responseAttributes[MetricsConstants.RESPONSE_TIME_MS]
        val forageErrorCode = responseAttributes[MetricsConstants.FORAGE_ERROR_CODE]
        val eventOutcome = eventOutcome

        if (responseTime == null || eventName != EventName.CUSTOMER_PERCEIVED_RESPONSE || eventOutcome == null) {
            metricsLogger?.e("[Metrics] Incomplete or missing response attributes. Could not log metric.")
            return
        }

        val vaultType = vaultType
        val userAction = userAction

        val forageErrorCodeOrNull = forageErrorCode ?: UnknownForageErrorCode.UNKNOWN

        metricsLogger?.i(
            "[Metrics] Customer perceived response time for $vaultType has been collected",
            attributes = mapOf(
                MetricsConstants.RESPONSE_TIME_MS to responseTime,
                MetricsConstants.VAULT_TYPE to vaultType,
                MetricsConstants.ACTION to userAction,
                MetricsConstants.EVENT_NAME to eventName,
                MetricsConstants.EVENT_OUTCOME to eventOutcome,
                MetricsConstants.FORAGE_ERROR_CODE to forageErrorCodeOrNull
            )
        )
    }
}

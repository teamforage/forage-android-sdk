package com.joinforage.forage.android.core.services.telemetry

import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse

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
    const val LOG_TYPE = "log_type"
}

internal enum class LogType(val value: String) {
    METRIC("metric");

    override fun toString(): String {
        return value
    }
}

internal enum class UnknownForageErrorCode(val value: String) {
    UNKNOWN("unknown");

    override fun toString(): String {
        return value
    }
}

internal enum class UserAction(val value: String) {
    BALANCE("balance"),
    CAPTURE("capture"),
    DEFER_CAPTURE("defer_capture"),
    REFUND("refund"),
    DEFER_REFUND("defer_refund");

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

internal abstract class ResponseMonitor<T>(metricsLogger: Log? = Log.getInstance()) {
    private var startTime: Long

    private var logger: Log? = null
    private var responseAttributes: MutableMap<String, Any> = mutableMapOf()

    init {
        logger = metricsLogger
        responseAttributes[MetricsConstants.LOG_TYPE] = LogType.METRIC
        startTime = System.nanoTime()
    }

    fun setPath(path: String): ResponseMonitor<T> {
        responseAttributes[MetricsConstants.PATH] = path
        return this
    }

    fun setMethod(method: String): ResponseMonitor<T> {
        responseAttributes[MetricsConstants.METHOD] = method
        return this
    }

    fun setHttpStatusCode(code: Int): ResponseMonitor<T> {
        responseAttributes[MetricsConstants.HTTP_STATUS] = code
        return this
    }

    fun setForageErrorCode(errorCode: String): ResponseMonitor<T> {
        responseAttributes[MetricsConstants.FORAGE_ERROR_CODE] = errorCode
        return this
    }

    fun logResult() {
        val end = System.nanoTime()
        responseAttributes[MetricsConstants.RESPONSE_TIME_MS] = calculateDuration(startTime, end)
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
internal class VaultProxyResponseMonitor(vault: VaultType, userAction: UserAction, metricsLogger: Log?) : ResponseMonitor<VaultProxyResponseMonitor>(metricsLogger) {
    private var vaultType: VaultType? = null
    private var userAction: UserAction? = null
    private var eventName: EventName = EventName.VAULT_RESPONSE

    init {
        this.vaultType = vault
        this.userAction = userAction
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
        val logType = responseAttributes[MetricsConstants.LOG_TYPE]

        if (path == null || method == null || httpStatus == null || responseTime == null || logType == null) {
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
                MetricsConstants.FORAGE_ERROR_CODE to forageErrorCodeOrNull,
                MetricsConstants.LOG_TYPE to logType
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
internal class CustomerPerceivedResponseMonitor(vault: VaultType, userAction: UserAction, metricsLogger: Log?) : ResponseMonitor<CustomerPerceivedResponseMonitor>(metricsLogger) {
    private var vaultType: VaultType? = null
    private var userAction: UserAction? = null
    private var eventOutcome: EventOutcome? = null
    private var eventName: EventName = EventName.CUSTOMER_PERCEIVED_RESPONSE

    init {
        this.vaultType = vault
        this.userAction = userAction
    }

    fun setEventOutcome(eventOutcome: EventOutcome): CustomerPerceivedResponseMonitor {
        this.eventOutcome = eventOutcome
        return this
    }

    /**
     * Determines the outcome of a Forage API response,
     * to report the measurement to the Telemetry service.
     *
     * This involves stopping the measurement timer,
     * marking the Metrics event as a success or failure,
     * and if the event is a failure, setting the Forage error code.
     */
    fun setEventOutcome(apiResponse: ForageApiResponse<String>): CustomerPerceivedResponseMonitor {
        val outcome = if (apiResponse is ForageApiResponse.Failure) {
            if (apiResponse.errors.isNotEmpty()) {
                setForageErrorCode(apiResponse.errors[0].code)
                setHttpStatusCode(apiResponse.errors[0].httpStatusCode)
            }
            EventOutcome.FAILURE
        } else {
            setHttpStatusCode(200)
            EventOutcome.SUCCESS
        }
        return setEventOutcome(outcome)
    }

    override fun logWithResponseAttributes(
        metricsLogger: Log?,
        responseAttributes: Map<String, Any>
    ) {
        val responseTime = responseAttributes[MetricsConstants.RESPONSE_TIME_MS]
        val forageErrorCode = responseAttributes[MetricsConstants.FORAGE_ERROR_CODE]
        val logType = responseAttributes[MetricsConstants.LOG_TYPE]
        val httpStatus = responseAttributes[MetricsConstants.HTTP_STATUS]
        val eventOutcome = eventOutcome

        if (responseTime == null || eventName != EventName.CUSTOMER_PERCEIVED_RESPONSE || eventOutcome == null || logType == null || httpStatus == null) {
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
                MetricsConstants.FORAGE_ERROR_CODE to forageErrorCodeOrNull,
                MetricsConstants.LOG_TYPE to logType,
                MetricsConstants.HTTP_STATUS to httpStatus
            )
        )
    }
}

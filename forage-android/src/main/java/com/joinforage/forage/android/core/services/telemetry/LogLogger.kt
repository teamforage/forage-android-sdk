package com.joinforage.forage.android.core.services.telemetry

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.network.error.ForageError
import org.json.JSONException
import org.json.JSONObject

internal fun extractTenantIdFromToken(
    base64Util: IBase64Util,
    forageConfig: ForageConfig
): String? {
    val token = forageConfig.sessionToken
    val parts = token.split(".")
    val firstPart = parts[0].substringAfter("_")
    val decodedFirstPart = String(base64Util.decode(firstPart))
    val jsonObject: JSONObject
    try {
        jsonObject = JSONObject(decodedFirstPart)
    } catch (e: JSONException) {
        return null
    }
    return jsonObject.getInt("t").toString() // the "t" key is the tenant id
}

internal abstract class LogLogger(
    open val logEngine: ILogEngine,
    base64Utils: IBase64Util,
    private var logAttrs: LogAttributes,
    private var prefix: String = ""
) {
    val traceId = logAttrs.traceId

    init {
        val tenantId = extractTenantIdFromToken(base64Utils, logAttrs.forageConfig)
        logAttrs = logAttrs.copy(tenantId = tenantId)
    }

    open fun setAction(action: UserAction) {
        logAttrs = logAttrs.copy(action = action)
        prefix = "$prefix[${action.value}]"
    }

    fun setPaymentMethodRef(paymentMethodRef: String) {
        logAttrs = logAttrs.copy(paymentMethodRef = paymentMethodRef)
    }

    fun d(msg: String) {
        logEngine.captureLog(Loggable.Debug(prefix, msg, logAttrs.toMap()))
    }
    fun i(msg: String) {
        logEngine.captureLog(Loggable.Info(prefix, msg, logAttrs.toMap()))
    }
    fun w(msg: String) {
        logEngine.captureLog(Loggable.Warn(prefix, msg, logAttrs.toMap()))
    }
    fun e(msg: String, throwable: Throwable? = null) {
        logEngine.captureLog(Loggable.Error(prefix, msg, throwable, logAttrs.toMap()))
    }
    fun logForageError(error: ForageError) {
        // a method to that will do .w or .e depending
        // on the error code returned to make it state out
        // more in our datadog logs
        TODO("not implemented.")
    }
    fun m(msg: String, metricAttrs: MetricAttributes) {
        val combinedAttrs = logAttrs.toMap() + metricAttrs.toMap()
        logEngine.captureLog(Loggable.Metric(prefix, msg, combinedAttrs))
    }

    fun unknownException(throwable: Throwable) {
        e("An unknown exception occurred", throwable)
    }
}

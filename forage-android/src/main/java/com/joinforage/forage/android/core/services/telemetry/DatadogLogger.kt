package com.joinforage.forage.android.core.services.telemetry

import com.joinforage.datadog.android.log.Logger

internal class DatadogEngine(val dd: Logger) : ILogEngine {
    override fun captureLog(loggable: Loggable) {
        val msg = loggable.toString()
        when (loggable) {
            is Loggable.Debug -> dd.d(msg, attributes = loggable.attrs)
            is Loggable.Info -> dd.i(msg, attributes = loggable.attrs)
            is Loggable.Metric -> dd.i(msg, attributes = loggable.attrs)
            is Loggable.Warn -> dd.w(msg, attributes = loggable.attrs)
            is Loggable.Error -> dd.e(msg, loggable.throwable, attributes = loggable.attrs)
        }
    }
}

internal class DatadogLogger(
    val dd: Logger,
    logAttrs: LogAttributes,
    prefix: String
) : LogLogger(
    DatadogEngine(dd),
    AndroidBase64Util(),
    logAttrs,
    prefix
) {

    init {
        dd.addTag(LogAttributes.AttributesKey.VERSION_CODE.key, logAttrs.versionCode)
    }

    override fun setAction(action: UserAction) {
        // we go out of our way to make set the user action tag
        // to help gather insights in Datadog
        dd.addTag(LogAttributes.AttributesKey.ACTION.key, action.value)
        super.setAction(action)
    }
}

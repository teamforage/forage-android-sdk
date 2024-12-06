package com.joinforage.forage.android.pos.integration.logger

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.UserAction
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.error.ForageError
import com.joinforage.forage.android.core.services.telemetry.MetricName
import com.joinforage.forage.android.core.services.telemetry.MetricOutcome
import com.joinforage.forage.android.core.services.telemetry.LogAttributes
import com.joinforage.forage.android.core.services.telemetry.MetricAttributes
import com.joinforage.forage.android.core.services.telemetry.extractTenantIdFromToken
import com.joinforage.forage.android.pos.integration.base64.JavaBase64Util

internal interface LogAttrsContainer {
    val noPM: Map<String, String>
    val all: Map<String, String>
}

internal interface MetricAttributesContainer {
    val vaultRes: Map<String, String>
    val cusPercep: Map<String, String>
}

internal data class LoggableAttributes(
    val logAttrs: LogAttrsContainer,
    val metricAttrs: MetricAttributesContainer
)

internal class LoggableAttributesFactory(
    private val forageConfig: ForageConfig,
    private val traceId: String,
    private val posTerminalId: String,
    private val paymentMethodRef: String
) {
    operator fun invoke(
        action: UserAction,
        httpStatus: Int = 200,
        metricOutcome: MetricOutcome = MetricOutcome.SUCCESS
    ): LoggableAttributes {
        val attrsSansPMRef = LogAttributes(
            forageConfig = forageConfig,
            traceId = traceId,
            posTerminalId = posTerminalId,
            action = action,
            tenantId = extractTenantIdFromToken(JavaBase64Util(), forageConfig)
        ).toMap()

        val allAttrs = attrsSansPMRef + mapOf(
            LogAttributes.AttributesKey.PAYMENT_METHOD_REF.key to paymentMethodRef
        )

        val vaultMetricAttrs = allAttrs + MetricAttributes(
            metricName = MetricName.VAULT_RESPONSE,
            httpStatus = httpStatus,
            metricOutcome = metricOutcome
        ).toMap()

        val custPercMetricAttrs = allAttrs + MetricAttributes(
            metricName = MetricName.CUSTOMER_PERCEIVED_RESPONSE,
            httpStatus = httpStatus,
            metricOutcome = metricOutcome
        ).toMap()

        return LoggableAttributes(
            logAttrs = object : LogAttrsContainer {
                override val noPM = attrsSansPMRef
                override val all = allAttrs
            },
            metricAttrs = object : MetricAttributesContainer {
                override val vaultRes = vaultMetricAttrs
                override val cusPercep = custPercMetricAttrs
            }
        )
    }

    operator fun invoke(
        action: UserAction,
        forageError: ForageError
    ): LoggableAttributes {
        val attrsSansPMRef = LogAttributes(
            forageConfig = forageConfig,
            traceId = traceId,
            posTerminalId = posTerminalId,
            action = action,
            tenantId = extractTenantIdFromToken(JavaBase64Util(), forageConfig)
        ).toMap()

        val allAttrs = attrsSansPMRef + mapOf(
            LogAttributes.AttributesKey.PAYMENT_METHOD_REF.key to paymentMethodRef
        )

        val vaultMetricAttrs = allAttrs + MetricAttributes(
            metricName = MetricName.VAULT_RESPONSE,
            httpStatus = forageError.httpStatusCode,
            metricOutcome = MetricOutcome.FAILURE,
            forageErrorCode = forageError.code
        ).toMap()

        val custPercMetricAttrs = allAttrs + MetricAttributes(
            metricName = MetricName.CUSTOMER_PERCEIVED_RESPONSE,
            httpStatus = forageError.httpStatusCode,
            metricOutcome = MetricOutcome.FAILURE,
            forageErrorCode = forageError.code
        ).toMap()

        return LoggableAttributes(
            logAttrs = object : LogAttrsContainer {
                override val noPM = attrsSansPMRef
                override val all = allAttrs
            },
            metricAttrs = object : MetricAttributesContainer {
                override val vaultRes = vaultMetricAttrs
                override val cusPercep = custPercMetricAttrs
            }
        )
    }

    operator fun invoke(action: UserAction, failure: ForageApiResponse.Failure) =
        invoke(action, failure.error)
}

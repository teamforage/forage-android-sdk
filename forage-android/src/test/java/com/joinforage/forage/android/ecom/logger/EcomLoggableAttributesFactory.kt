package com.joinforage.forage.android.ecom.logger

import com.joinforage.forage.android.core.base64.JavaBase64Util
import com.joinforage.forage.android.core.logger.LogAttrsContainer
import com.joinforage.forage.android.core.logger.LoggableAttributes
import com.joinforage.forage.android.core.logger.MetricAttributesContainer
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.error.ForageError
import com.joinforage.forage.android.core.services.telemetry.LogAttributes
import com.joinforage.forage.android.core.services.telemetry.MetricAttributes
import com.joinforage.forage.android.core.services.telemetry.MetricName
import com.joinforage.forage.android.core.services.telemetry.MetricOutcome
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.telemetry.extractTenantIdFromToken


internal class EcomLoggableAttributesFactory(
    private val forageConfig: ForageConfig,
    private val traceId: String,
    private val paymentMethodRef: String? = null
) {
    operator fun invoke(
        action: UserAction,
        httpStatus: Int = 200,
        metricOutcome: MetricOutcome = MetricOutcome.SUCCESS
    ): LoggableAttributes {
        val attrsSansPMRef = LogAttributes(
            forageConfig = forageConfig,
            traceId = traceId,
            action = action,
            tenantId = extractTenantIdFromToken(JavaBase64Util(), forageConfig)
        ).toMap()

        val allAttrs = attrsSansPMRef/* + mapOf(
            LogAttributes.AttributesKey.PAYMENT_METHOD_REF.key to paymentMethodRef
        )*/

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
            action = action,
            tenantId = extractTenantIdFromToken(JavaBase64Util(), forageConfig)
        ).toMap()

        val allAttrs = attrsSansPMRef/* + mapOf(
            LogAttributes.AttributesKey.PAYMENT_METHOD_REF.key to paymentMethodRef
        )*/

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

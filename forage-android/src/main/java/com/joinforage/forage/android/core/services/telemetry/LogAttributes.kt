package com.joinforage.forage.android.core.services.telemetry

import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.ForageConfig

internal data class LogAttributes(
    val forageConfig: ForageConfig,
    val traceId: String,
    val posTerminalId: String? = null,
    val customerId: String? = null,
    val action: UserAction? = null,
    val paymentRef: String? = null,
    val paymentMethodRef: String? = null,
    val tenantId: String? = null
) : ILoggableAttributes {
    private val env = EnvConfig.fromForageConfig(forageConfig)
    val versionCode = env.PUBLISH_VERSION
    val eventType = LogType.LOG
    val merchantRef = forageConfig.merchantId

    enum class AttributesKey(val key: String) {
        VERSION_CODE("version_code"),
        MERCHANT_REF("merchant_ref"),
        TENANT_ID("tenant_id"),
        TRACE_ID("trace_id"),
        POS_TERMINAL_ID("pos_terminal_id"),
        CUSTOMER_ID("customer_id"),
        ACTION("action"),
        PAYMENT_REF("payment_ref"),
        PAYMENT_METHOD_REF("payment_method_ref"),
        LOG_TYPE("log_type");
    }

    override fun toMap(): Map<String, String> = mapOf(
        AttributesKey.VERSION_CODE.key to versionCode,
        AttributesKey.MERCHANT_REF.key to merchantRef,
        AttributesKey.TENANT_ID.key to tenantId,
        AttributesKey.TRACE_ID.key to traceId,
        AttributesKey.POS_TERMINAL_ID.key to posTerminalId,
        AttributesKey.CUSTOMER_ID.key to customerId,
        AttributesKey.ACTION.key to action?.value,
        AttributesKey.PAYMENT_REF.key to paymentRef,
        AttributesKey.PAYMENT_METHOD_REF.key to paymentMethodRef,
        AttributesKey.LOG_TYPE.key to eventType.value
    ).filterValues { it != null } // Filter out entries with null values
        .mapValues { it.value!! } // Safely unwrap non-null values
}

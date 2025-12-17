package com.joinforage.forage.android.core.services.forageapi.requests

import com.joinforage.forage.android.BuildConfig

internal data class Headers(
    val posTerminalId: String? = null,
    val xKey: String? = null,
    val merchantAccount: String? = null,
    val idempotencyKey: String? = null,
    val traceId: String? = null,
    val authorization: String? = null,
    val apiVersion: ApiVersion? = null,
    val contentType: ContentType? = null
) : Iterable<Pair<String, String>> {
    fun setXKey(xKey: String) = this.copy(xKey = xKey)
    fun setMerchantAccount(merchantAccount: String) = this.copy(merchantAccount = merchantAccount)
    fun setIdempotencyKey(idempotencyKey: String) = this.copy(idempotencyKey = idempotencyKey)
    fun setTraceId(traceId: String) = this.copy(traceId = traceId)
    fun setAuthorization(authorization: String) = this.copy(authorization = authorization)
    fun setApiVersion(apiVersion: ApiVersion) = this.copy(apiVersion = apiVersion)
    fun setContentType(contentType: ContentType) = this.copy(contentType = contentType)

    enum class HeaderKey(val key: String) {
        X_FORAGE_ANDROID_SDK_VERSION("X-Forage-Android-Sdk-Version"),
        X_TERMINAL_ID("X-TERMINAL-ID"),
        X_KEY("X-KEY"),
        MERCHANT_ACCOUNT("Merchant-Account"),
        IDEMPOTENCY_KEY("IDEMPOTENCY-KEY"),
        TRACE_ID("x-datadog-trace-id"),
        AUTHORIZATION("Authorization"),
        API_VERSION("API-VERSION"),
        BT_PROXY_KEY("BT-PROXY-KEY"),
        CONTENT_TYPE("Content-Type")
    }

    enum class ContentType(val value: String, val mediaType: String) {
        APPLICATION_JSON("application/json", "application/json; charset=utf-8"),
        APPLICATION_FORM_URLENCODED("application/x-www-form-urlencoded", "application/x-www-form-urlencoded")
    }

    enum class ApiVersion(val value: String) {
        V_DEFAULT("default"),
        V_2023_05_15("2023-05-15"),
        V_2024_01_08("2024-01-08")
    }

    fun toMap(): Map<String, String> = mapOf(
        HeaderKey.X_FORAGE_ANDROID_SDK_VERSION.key to BuildConfig.PUBLISH_VERSION,
        HeaderKey.X_TERMINAL_ID.key to posTerminalId,
        HeaderKey.X_KEY.key to xKey,
        HeaderKey.MERCHANT_ACCOUNT.key to merchantAccount,
        HeaderKey.IDEMPOTENCY_KEY.key to idempotencyKey,
        HeaderKey.TRACE_ID.key to traceId,
        HeaderKey.AUTHORIZATION.key to authorization,
        HeaderKey.API_VERSION.key to apiVersion?.value,
        HeaderKey.CONTENT_TYPE.key to contentType?.value
    ).filterValues { it != null } // Filter out entries with null values
        .mapValues { it.value!! } // Safely unwrap non-null values

    // Make the class iterable
    override fun iterator(): Iterator<Pair<String, String>> {
        return toMap().entries.map { it.key to it.value }.iterator()
    }
}

package com.joinforage.forage.android.collect

import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.ForageConstants
import com.joinforage.forage.android.network.model.ForageApiResponse
import java.util.UUID

internal object CollectorConstants {
    const val TOKEN_DELIMITER = ","
}

internal abstract class PinCollector {
    abstract suspend fun submitBalanceCheck(
        paymentMethodRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String>

    abstract suspend fun submitPaymentCapture(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String>

    abstract suspend fun submitDeferPaymentCapture(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String>

    abstract suspend fun submitPosRefund(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String,
        terminalId: String,
        amount: String,
        reason: String,
        metadata: Map<String, Any>? = null
    ): ForageApiResponse<String>

    abstract fun parseEncryptionKey(
        encryptionKeys: EncryptionKeys
    ): String

    abstract fun parseVaultToken(
        paymentMethod: PaymentMethod
    ): String

    abstract fun getVaultType(): VaultType

    fun buildHeaders(
        merchantAccount: String,
        encryptionKey: String,
        idempotencyKey: String = UUID.randomUUID().toString(),
        traceId: String = ""
    ): HashMap<String, String> {
        val headers = HashMap<String, String>()
        headers[ForageConstants.Headers.X_KEY] = encryptionKey
        headers[ForageConstants.Headers.MERCHANT_ACCOUNT] = merchantAccount
        headers[ForageConstants.Headers.IDEMPOTENCY_KEY] = idempotencyKey
        headers[ForageConstants.Headers.TRACE_ID] = traceId
        return headers
    }

    fun balancePath(paymentMethodRef: String) =
        "/api/payment_methods/$paymentMethodRef/balance/"

    fun capturePaymentPath(paymentRef: String) =
        "/api/payments/$paymentRef/capture/"

    fun deferPaymentCapturePath(paymentRef: String) =
        "/api/payments/$paymentRef/collect_pin/"

    fun posRefundPath(paymentRef: String) =
        "/api/payments/$paymentRef/refunds/?type=pos"
}

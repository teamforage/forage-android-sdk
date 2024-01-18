package com.joinforage.forage.android.collect

import android.content.Context
import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.core.telemetry.UserAction
import com.joinforage.forage.android.core.telemetry.VaultProxyResponseMonitor
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.ForageConstants
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.network.model.UnknownErrorApiResponse
import com.joinforage.forage.android.ui.ForagePINEditText

internal val IncompletePinError = ForageApiResponse.Failure.fromError(
    ForageError(400, "user_error", "Invalid EBT Card PIN entered. Please enter your 4-digit PIN.")
)

internal interface VaultSubmitter {
    suspend fun submit(
        encryptionKeys: EncryptionKeys,
        idempotencyKey: String,
        merchantId: String,
        path: String,
        paymentMethod: PaymentMethod,
        userAction: UserAction
    ): ForageApiResponse<String>

    fun getVaultType(): VaultType
}

internal abstract class AbstractVaultSubmitter<VaultResponse>(
    protected val foragePinEditText: ForagePINEditText,
    protected val logger: Log = Log.getInstance(),
    protected val context: Context,
    private val vaultType: VaultType
) : VaultSubmitter {
    // interface methods
    override suspend fun submit(
        encryptionKeys: EncryptionKeys,
        idempotencyKey: String,
        merchantId: String,
        path: String,
        paymentMethod: PaymentMethod,
        userAction: UserAction
    ): ForageApiResponse<String> {
        logger.addAttribute("payment_method_ref", paymentMethod.ref)
            .addAttribute("merchant_ref", merchantId)
        logger.i("[$vaultType] Sending $userAction request to $vaultType")

        // If the PIN isn't valid (less than 4 numbers) then return a response here.
        if (!foragePinEditText.getElementState().isComplete) {
            logger.w("[$vaultType] User attempted to submit an incomplete PIN")
            return IncompletePinError
        }

        val vaultToken = getVaultToken(paymentMethod)
        val encryptionKey = parseEncryptionKey(encryptionKeys)

        // if a vault provider is missing a token, we will
        // gracefully fail here
        if (vaultToken.isNullOrEmpty()) {
            logger.e("Vault token is missing from Payments API response")
            return UnknownErrorApiResponse
        }

        // ========= USED FOR REPORTING IMPORTANT METRICS =========
        val proxyResponseMonitor = VaultProxyResponseMonitor(
            vault = vaultType,
            userAction = userAction,
            metricsLogger = logger
        )
        proxyResponseMonitor
            .setPath(path)
            .setMethod("POST")
            .start()

        val vaultProxyRequest = buildProxyRequest(
            encryptionKey = encryptionKey,
            idempotencyKey = idempotencyKey,
            merchantId = merchantId,
            vaultToken = vaultToken
        ).setPath(path)
        val forageResponse = submitProxyRequest(vaultProxyRequest = vaultProxyRequest)
        proxyResponseMonitor.end()

        // FNS requirement to clear the PIN after each submission
        foragePinEditText.clearText()

        val httpStatusCode = when (forageResponse) {
            is ForageApiResponse.Success -> 200
            is ForageApiResponse.Failure -> forageResponse.errors[0].httpStatusCode
        }
        proxyResponseMonitor.setHttpStatusCode(httpStatusCode).logResult()
        // ==========================================================

        return forageResponse
    }

    override fun getVaultType(): VaultType {
        return vaultType
    }

    // abstract methods
    internal abstract fun parseEncryptionKey(encryptionKeys: EncryptionKeys): String
    internal abstract suspend fun submitProxyRequest(vaultProxyRequest: VaultProxyRequest): ForageApiResponse<String>
    internal abstract fun getVaultToken(paymentMethod: PaymentMethod): String?

    internal abstract fun toVaultErrorOrNull(vaultResponse: VaultResponse): ForageApiResponse.Failure?
    abstract fun toForageErrorOrNull(vaultResponse: VaultResponse): ForageApiResponse.Failure?
    abstract fun toForageSuccessOrNull(vaultResponse: VaultResponse): ForageApiResponse.Success<String>?
    abstract fun parseVaultError(vaultResponse: VaultResponse): String

    // concrete methods
    protected open fun buildProxyRequest(
        encryptionKey: String,
        idempotencyKey: String,
        merchantId: String,
        vaultToken: String
    ) = VaultProxyRequest.emptyRequest()
        .setHeader(ForageConstants.Headers.X_KEY, encryptionKey)
        .setHeader(ForageConstants.Headers.MERCHANT_ACCOUNT, merchantId)
        .setHeader(ForageConstants.Headers.IDEMPOTENCY_KEY, idempotencyKey)
        .setHeader(ForageConstants.Headers.TRACE_ID, logger.getTraceIdValue())
        .setToken(vaultToken)

    // PaymentMethod.card.token is in the comma-separated format <vgs-token>,<basis-theory-token>
    protected fun pickVaultTokenByIndex(paymentMethod: PaymentMethod, index: Int): String? {
        val tokensString = paymentMethod.card.token
        val tokensList = tokensString.split(TOKEN_DELIMITER)

        val noTokenStoredInVault = tokensList.size <= index
        if (noTokenStoredInVault) return null

        return tokensList[index]
    }

    protected fun vaultToForageResponse(vaultResponse: VaultResponse): ForageApiResponse<String> {
        if (vaultResponse == null) {
            logger.e("[$vaultType] Received null response from $vaultType")
            return UnknownErrorApiResponse
        }

        val vaultError = toVaultErrorOrNull(vaultResponse)
        if (vaultError != null) {
            val forageErr = parseVaultError(vaultResponse)
            logger.e("[$vaultType] Received error from $vaultType: $forageErr")
            return vaultError
        }

        val forageApiErrorResponse = toForageErrorOrNull(vaultResponse)
        if (forageApiErrorResponse != null) {
            val firstError = forageApiErrorResponse.errors[0]
            logger.e("[$vaultType] Received error from $vaultType: $firstError")
            return forageApiErrorResponse
        }

        val forageApiSuccess = toForageSuccessOrNull(vaultResponse)
        if (forageApiSuccess != null) {
            logger.i("[$vaultType] Received successful response from $vaultType")
            return forageApiSuccess
        }
        logger.e("[$vaultType] Received malformed response from $vaultType")

        return UnknownErrorApiResponse
    }

    internal companion object {
        const val TOKEN_DELIMITER = ","

        internal fun balancePath(paymentMethodRef: String) =
            "/api/payment_methods/$paymentMethodRef/balance/"

        internal fun capturePaymentPath(paymentRef: String) =
            "/api/payments/$paymentRef/capture/"

        internal fun deferPaymentCapturePath(paymentRef: String) =
            "/api/payments/$paymentRef/collect_pin/"
    }
}

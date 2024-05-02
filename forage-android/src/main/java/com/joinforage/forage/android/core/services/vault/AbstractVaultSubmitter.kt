package com.joinforage.forage.android.core.services.vault

import android.content.Context
import com.joinforage.forage.android.core.services.launchdarkly.VaultType
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.telemetry.VaultProxyResponseMonitor
import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeys
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.ForageConstants
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.ForageError
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.ecom.services.vault.bt.BasisTheoryPinSubmitter
import com.joinforage.forage.android.ecom.ui.ForagePINEditText
import com.joinforage.forage.android.ecom.services.vault.vgs.VgsPinSubmitter

internal val IncompletePinError = ForageApiResponse.Failure.fromError(
    ForageError(400, "user_error", "Invalid EBT Card PIN entered. Please enter your 4-digit PIN.")
)

internal open class VaultSubmitterParams(
    open val encryptionKeys: EncryptionKeys,
    open val idempotencyKey: String,
    open val merchantId: String,
    open val path: String,
    open val paymentMethod: PaymentMethod,
    open val sessionToken: String,
    open val userAction: UserAction
)

internal interface VaultSubmitter {
    suspend fun submit(params: VaultSubmitterParams): ForageApiResponse<String>

    fun getVaultType(): VaultType
}

internal abstract class AbstractVaultSubmitter<VaultResponse>(
    protected val context: Context,
    protected val foragePinEditText: ForagePINEditText,
    protected val logger: Log,
    private val vaultType: VaultType
) : VaultSubmitter {
    // interface methods
    override suspend fun submit(params: VaultSubmitterParams): ForageApiResponse<String> {
        logger.addAttribute("payment_method_ref", params.paymentMethod.ref)
            .addAttribute("merchant_ref", params.merchantId)
        logger.i("[$vaultType] Sending ${params.userAction} request to $vaultType")

        // If the PIN isn't valid (less than 4 numbers) then return a response here.
        if (!foragePinEditText.getElementState().isComplete) {
            logger.w("[$vaultType] User attempted to submit an incomplete PIN")
            return IncompletePinError
        }

        val vaultToken = getVaultToken(params.paymentMethod)
        val encryptionKey = parseEncryptionKey(params.encryptionKeys)

        // if a vault provider is missing a token, we will
        // gracefully fail here
        if (vaultToken.isNullOrEmpty()) {
            logger.e("Vault token is missing from Payments API response")
            return UnknownErrorApiResponse
        }

        // ========= USED FOR REPORTING IMPORTANT METRICS =========
        val proxyResponseMonitor = VaultProxyResponseMonitor(
            vault = vaultType,
            userAction = params.userAction,
            metricsLogger = logger
        )
        proxyResponseMonitor
            .setPath(params.path)
            .setMethod("POST")
            .start()
        // ==========================================================

        val vaultProxyRequest = buildProxyRequest(
            params = params,
            encryptionKey = encryptionKey,
            vaultToken = vaultToken
        ).setPath(params.path).setParams(params)

        val forageResponse = submitProxyRequest(vaultProxyRequest)
        proxyResponseMonitor.end()

        // FNS requirement to clear the PIN after each submission
        foragePinEditText.clearText()

        if (forageResponse is ForageApiResponse.Failure && forageResponse.errors.isNotEmpty()) {
            val forageError = forageResponse.errors.first()
            proxyResponseMonitor.setForageErrorCode(forageError.code)
            proxyResponseMonitor.setHttpStatusCode(forageError.httpStatusCode)
        } else {
            proxyResponseMonitor.setHttpStatusCode(200)
        }
        proxyResponseMonitor.logResult()

        return forageResponse
    }

    override fun getVaultType(): VaultType {
        return vaultType
    }

    // abstract methods
    internal abstract fun parseEncryptionKey(encryptionKeys: EncryptionKeys): String
    internal abstract suspend fun submitProxyRequest(vaultProxyRequest: VaultProxyRequest): ForageApiResponse<String>
    internal abstract fun getVaultToken(paymentMethod: PaymentMethod): String?

    /**
     * @return [UnknownErrorApiResponse] if the response is a vault error, or null if it is not
     */
    internal abstract fun toVaultErrorOrNull(vaultResponse: VaultResponse): ForageApiResponse.Failure?
    internal abstract fun toForageErrorOrNull(vaultResponse: VaultResponse): ForageApiResponse.Failure?
    internal abstract fun toForageSuccessOrNull(vaultResponse: VaultResponse): ForageApiResponse.Success<String>?

    /**
     * @return A string containing the raw error details of the vault error.
     * To be used for internal error reporting.
     */
    internal abstract fun parseVaultErrorMessage(vaultResponse: VaultResponse): String

    // concrete methods
    protected open fun buildProxyRequest(
        params: VaultSubmitterParams,
        encryptionKey: String,
        vaultToken: String
    ) = VaultProxyRequest.emptyRequest()
        .setHeader(ForageConstants.Headers.X_KEY, encryptionKey)
        .setHeader(ForageConstants.Headers.MERCHANT_ACCOUNT, params.merchantId)
        .setHeader(ForageConstants.Headers.IDEMPOTENCY_KEY, params.idempotencyKey)
        .setHeader(ForageConstants.Headers.TRACE_ID, logger.getTraceIdValue())
        .setHeader(ForageConstants.Headers.API_VERSION, "default")
        .setToken(vaultToken)

    // PaymentMethod.card.token is in the comma-separated format <vgs-token>,<basis-theory-token>,<forage-token>
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
            val rawVaultError = parseVaultErrorMessage(vaultResponse)
            logger.e("[$vaultType] Received error from $vaultType: $rawVaultError")
            return vaultError
        }

        val forageApiErrorResponse = toForageErrorOrNull(vaultResponse)
        if (forageApiErrorResponse != null) {
            val firstError = forageApiErrorResponse.errors[0]
            logger.e("[$vaultType] Received ForageError from $vaultType: $firstError")
            return forageApiErrorResponse
        }

        val forageApiSuccess = toForageSuccessOrNull(vaultResponse)
        if (forageApiSuccess != null) {
            logger.i("[$vaultType] Received successful response from $vaultType")
            return forageApiSuccess
        }
        logger.e("[$vaultType] Received malformed response from $vaultType: $vaultResponse")

        return UnknownErrorApiResponse
    }

    protected fun buildBaseRequestBody(vaultProxyRequest: VaultProxyRequest): HashMap<String, Any> {
        return hashMapOf(
            ForageConstants.RequestBody.CARD_NUMBER_TOKEN to vaultProxyRequest.vaultToken
        )
    }

    internal companion object {
        internal fun create(foragePinEditText: ForagePINEditText, logger: Log): VaultSubmitter {
            val vaultType = foragePinEditText.getVaultType()
            if (vaultType == VaultType.BT_VAULT_TYPE) {
                return BasisTheoryPinSubmitter(
                    context = foragePinEditText.context,
                    foragePinEditText = foragePinEditText,
                    logger = logger
                )
            }
            return VgsPinSubmitter(
                context = foragePinEditText.context,
                foragePinEditText = foragePinEditText,
                logger = logger
            )
        }

        const val TOKEN_DELIMITER = ","

        internal fun balancePath(paymentMethodRef: String) =
            "/api/payment_methods/$paymentMethodRef/balance/"

        internal fun capturePaymentPath(paymentRef: String) =
            "/api/payments/$paymentRef/capture/"

        internal fun deferPaymentCapturePath(paymentRef: String) =
            "/api/payments/$paymentRef/collect_pin/"

    }
}

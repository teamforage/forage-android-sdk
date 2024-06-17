package com.joinforage.forage.android.core.services.vault

import com.joinforage.forage.android.core.services.ForageConstants
import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeys
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.ForageError
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtCard
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.telemetry.VaultProxyResponseMonitor

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

internal interface SecurePinCollector {
    fun clearText()
    fun isComplete(): Boolean
}

internal interface VaultSubmitter {
    suspend fun submit(params: VaultSubmitterParams): ForageApiResponse<String>
}

internal abstract class AbstractVaultSubmitter(
    protected val collector: SecurePinCollector,
    protected val logger: Log
) : VaultSubmitter {

    abstract val vaultType: VaultType

    // interface methods
    override suspend fun submit(params: VaultSubmitterParams): ForageApiResponse<String> {
        logger.addAttribute("payment_method_ref", params.paymentMethod.ref)
            .addAttribute("merchant_ref", params.merchantId)
        logger.i("[$vaultType] Sending ${params.userAction} request to $vaultType")

        // If the PIN isn't valid (less than 4 numbers) then return a response here.
        if (!collector.isComplete()) {
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
        collector.clearText()

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

    // abstract methods
    internal abstract fun parseEncryptionKey(encryptionKeys: EncryptionKeys): String
    internal abstract suspend fun submitProxyRequest(vaultProxyRequest: VaultProxyRequest): ForageApiResponse<String>
    internal abstract fun getVaultToken(paymentMethod: PaymentMethod): String?

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
        .setHeader(ForageConstants.Headers.SESSION_TOKEN, "${ForageConstants.Headers.BEARER} ${params.sessionToken}")
        .setToken(vaultToken)

    // PaymentMethod.card.token is in the comma-separated format <vgs-token>,<basis-theory-token>,<forage-token>
    protected fun pickVaultTokenByIndex(paymentMethod: PaymentMethod, index: Int): String? {
        val tokensString = (paymentMethod.card as EbtCard).token
        val tokensList = tokensString.split(TOKEN_DELIMITER)

        val noTokenStoredInVault = tokensList.size <= index
        if (noTokenStoredInVault) return null

        return tokensList[index]
    }

    // TODO: this method should really live on the VaultResponseParser
    //  interface's companion object. The specifics how how a parser
    //  get's transformed into a ForageApiResponse are not something
    //  the AbstractVaultSubmitter needs to know about
    protected fun vaultToForageResponse(parser: VaultResponseParser): ForageApiResponse<String> {
        if (parser.isNullResponse) {
            logger.e("[$vaultType] Received null response from $vaultType")
            return UnknownErrorApiResponse
        }

        val vaultError = parser.vaultError
        if (vaultError != null) {
            logger.e("[$vaultType] Received error from $vaultType: ${parser.vaultErrorMsg}")
            return vaultError
        }

        val forageError = parser.forageError
        if (forageError != null) {
            val firstError = forageError.errors[0]
            logger.e("[$vaultType] Received ForageError from $vaultType: $firstError")
            return forageError
        }

        val successfulResponse = parser.successfulResponse
                if (successfulResponse != null) {
            logger.i("[$vaultType] Received successful response from $vaultType")
            return successfulResponse
        }

        logger.e("[$vaultType] Received malformed response from $vaultType: ${parser.rawResponse}")
        return UnknownErrorApiResponse
    }

    protected fun buildBaseRequestBody(vaultProxyRequest: VaultProxyRequest): HashMap<String, Any> {
        return hashMapOf(
            ForageConstants.RequestBody.CARD_NUMBER_TOKEN to vaultProxyRequest.vaultToken
        )
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

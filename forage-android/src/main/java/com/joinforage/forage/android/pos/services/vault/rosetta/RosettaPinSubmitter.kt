package com.joinforage.forage.android.pos.services.vault.rosetta

import android.annotation.SuppressLint
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.ForageConstants
import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.services.addPathSegmentsSafe
import com.joinforage.forage.android.core.services.addTrailingSlash
import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeys
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.NetworkService
import com.joinforage.forage.android.core.services.forageapi.network.OkHttpClientBuilder
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtCard
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.core.services.vault.SecurePinCollector
import com.joinforage.forage.android.core.services.vault.VaultProxyRequest
import com.joinforage.forage.android.core.services.vault.VaultSubmitterParams
import com.joinforage.forage.android.pos.services.encryption.dukpt.DukptService
import com.joinforage.forage.android.pos.services.encryption.iso4.PinBlockIso4
import com.joinforage.forage.android.pos.services.encryption.storage.AndroidKeyStoreKeyRegisters
import com.joinforage.forage.android.pos.services.encryption.storage.KsnFileManager
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

internal data class PinTranslationParams(
    val encryptedPinBlock: String,
    val keySerialNumber: String,
    val txnCounter: String
)

internal class RosettaPinSubmitter(
    private val plainTextPin: String,
    collector: SecurePinCollector,
    private val envConfig: EnvConfig,
    logger: Log,
    private val vaultUrlBuilder: ((String) -> HttpUrl) = { path -> buildVaultUrl(envConfig, path) }
) : AbstractVaultSubmitter(
    collector = collector,
    logger = logger
) {
    override val vaultType = VaultType.FORAGE_VAULT_TYPE

    // x-key header is not applicable to forage
    override fun parseEncryptionKey(encryptionKeys: EncryptionKeys): String {
        return ""
    }

    private fun buildRequestBody(vaultProxyRequest: VaultProxyRequest): HashMap<String, Any> {
        val baseRequestBody = buildBaseRequestBody(vaultProxyRequest)
        return when (vaultProxyRequest.params) {
            is PosBalanceVaultSubmitterParams -> buildPosBalanceCheckRequestBody(baseRequestBody, vaultProxyRequest.params)
            is PosRefundVaultSubmitterParams -> buildPosRefundRequestBody(baseRequestBody, vaultProxyRequest.params)
            else -> baseRequestBody
        }
    }

    private fun buildPosRefundRequestBody(
        body: HashMap<String, Any>,
        posParams: PosRefundVaultSubmitterParams
    ): HashMap<String, Any> {
        body[ForageConstants.RequestBody.AMOUNT] = posParams.refundParams.amount
        body[ForageConstants.RequestBody.REASON] = posParams.refundParams.reason
        body[ForageConstants.RequestBody.METADATA] = posParams.refundParams.metadata ?: HashMap<String, String>()
        body[ForageConstants.RequestBody.POS_TERMINAL] = hashMapOf(
            ForageConstants.RequestBody.PROVIDER_TERMINAL_ID to posParams.posTerminalId
        )
        return body
    }

    private fun buildPosBalanceCheckRequestBody(
        body: HashMap<String, Any>,
        posParams: PosBalanceVaultSubmitterParams
    ): HashMap<String, Any> {
        body[ForageConstants.RequestBody.POS_TERMINAL] = hashMapOf(
            ForageConstants.RequestBody.PROVIDER_TERMINAL_ID to posParams.posTerminalId
        )
        return body
    }

    override suspend fun submitProxyRequest(vaultProxyRequest: VaultProxyRequest): ForageApiResponse<String> {
        return try {
            val apiUrl = buildVaultUrl(envConfig, vaultProxyRequest.path)
            val baseRequestBody = buildRequestBody(vaultProxyRequest)

            if (vaultProxyRequest.params == null) {
                logger.e("vaultProxyRequest params were null")
                return UnknownErrorApiResponse
            }
            val plainTextPan = (vaultProxyRequest.params.paymentMethod.card as EbtCard?)?.number
            val headerValues = vaultProxyRequest.params

            if (plainTextPan == null) {
                logger.e("PaymentMethod.card.number was null")
                return UnknownErrorApiResponse
            }

            val pinTranslationParams = buildPinTranslationParams(vaultProxyRequest)
            val requestBody = buildForageVaultRequestBody(pinTranslationParams, baseRequestBody)
            val request = Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build()

            val okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                sessionToken = headerValues.sessionToken,
                merchantId = headerValues.merchantId,
                traceId = logger.getTraceIdValue(),
                idempotencyKey = headerValues.idempotencyKey,
                apiVersion = "2024-01-08"
            )

            val vaultService: NetworkService = object : NetworkService(okHttpClient, logger) {}
            val rawForageVaultResponse = vaultService.convertCallbackToCoroutine(request)

            vaultToForageResponse(RosettaResponseParser(rawForageVaultResponse))
        } catch (e: Exception) {
            logger.e("Failed to send request to Forage Vault.", e)
            UnknownErrorApiResponse
        }
    }

    override fun buildProxyRequest(
        params: VaultSubmitterParams,
        encryptionKey: String,
        vaultToken: String
    ) = super
        .buildProxyRequest(
            params = params,
            encryptionKey = encryptionKey,
            vaultToken = vaultToken
        )
        .setHeader(ForageConstants.Headers.CONTENT_TYPE, "application/json")

    override fun getVaultToken(paymentMethod: PaymentMethod): String? =
        pickVaultTokenByIndex(paymentMethod, 2)

    /**
     * The `init` method of the SDK ensures the client is using Android M (23+) or later.
     */
    @SuppressLint("NewApi")
    @Throws(Exception::class, IllegalArgumentException::class)
    private fun buildPinTranslationParams(vaultProxyRequest: VaultProxyRequest): PinTranslationParams {
        val plainTextPan = (vaultProxyRequest.params?.paymentMethod?.card as EbtCard?)?.number
            ?: throw IllegalArgumentException("PaymentMethod.card.number was null")

        val ksn = ksnFileManager!!.readAll() ?: throw IllegalArgumentException("Failed to get KSN from file")

        try {
            val dukptService = DukptService(ksn = ksn, keyRegisters = AndroidKeyStoreKeyRegisters())
            val (workingKey, latestKsn) = dukptService.generateWorkingKey()
            ksnFileManager!!.updateKsn(latestKsn)

            val encryptedPinBlock = PinBlockIso4(plainTextPan, plainTextPin, workingKey).contents.toHexString().uppercase()

            return PinTranslationParams(
                encryptedPinBlock = encryptedPinBlock,
                keySerialNumber = latestKsn.apcKsn,
                txnCounter = latestKsn.workingKeyTxCountAsBigEndian8CharHex
            )
        } catch (e: Exception) {
            throw Exception("Failed to encrypt PIN using dukpt service", e)
        }
    }

    companion object {
        // STOPGAP: global static var needed to read from the KSN file.
        internal var ksnFileManager: KsnFileManager? = null

        private fun buildVaultUrl(envConfig: EnvConfig, path: String): HttpUrl =
            envConfig.vaultBaseUrl.toHttpUrlOrNull()!!
                .newBuilder()
                .addPathSegment("proxy")
                .addPathSegmentsSafe(path)
                .addTrailingSlash()
                .build()

        private fun buildForageVaultRequestBody(
            pinTranslationParams: PinTranslationParams,
            baseRequestBody: Map<String, Any>
        ): RequestBody {
            val jsonBody = JSONObject(baseRequestBody)
            jsonBody.put("pin", pinTranslationParams.encryptedPinBlock)
            jsonBody.put(ForageConstants.RequestBody.KSN, pinTranslationParams.keySerialNumber)
            jsonBody.put(ForageConstants.RequestBody.TXN_COUNTER, pinTranslationParams.txnCounter)

            val mediaType = "application/json".toMediaTypeOrNull()
            return jsonBody.toString().toRequestBody(mediaType)
        }
    }
}

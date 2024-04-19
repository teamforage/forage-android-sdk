package com.joinforage.forage.android.vault

import android.annotation.SuppressLint
import android.content.Context
import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.addPathSegmentsSafe
import com.joinforage.forage.android.addTrailingSlash
import com.joinforage.forage.android.core.ForagePinElement
import com.joinforage.forage.android.core.StopgapGlobalState
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.ForageConstants
import com.joinforage.forage.android.network.NetworkService
import com.joinforage.forage.android.network.OkHttpClientBuilder
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.UnknownErrorApiResponse
import com.joinforage.forage.android.pos.encryption.dukpt.DukptService
import com.joinforage.forage.android.pos.encryption.iso4.PinBlockIso4
import com.joinforage.forage.android.pos.encryption.storage.AndroidKeyStoreKeyRegisters
import com.joinforage.forage.android.pos.encryption.storage.KsnFileManager
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

internal class ForagePinSubmitter(
    context: Context,
    foragePinEditText: ForagePinElement,
    logger: Log
) : AbstractVaultSubmitter<ForageApiResponse<String>>(
    context = context,
    foragePinEditText = foragePinEditText,
    logger = logger,
    vaultType = VaultType.FORAGE_VAULT_TYPE
) {
    // x-key header is not applicable to forage
    override fun parseEncryptionKey(encryptionKeys: EncryptionKeys): String {
        return ""
    }
    override suspend fun submitProxyRequest(vaultProxyRequest: VaultProxyRequest): ForageApiResponse<String> {
        return try {
            val apiUrl = buildVaultUrl(vaultProxyRequest.path)

            val baseRequestBody = buildRequestBody(vaultProxyRequest)
            val plainTextPan = vaultProxyRequest.params?.paymentMethod?.card?.number

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

            val headerValues = vaultProxyRequest.params

            val okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                sessionToken = headerValues.sessionToken,
                merchantId = headerValues.merchantId,
                traceId = logger.getTraceIdValue(),
                idempotencyKey = headerValues.idempotencyKey
            )

            val vaultService: NetworkService = object : NetworkService(okHttpClient, logger) {}
            val rawForageVaultResponse = vaultService.convertCallbackToCoroutine(request)

            vaultToForageResponse(rawForageVaultResponse)
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

    override fun parseVaultErrorMessage(vaultResponse: ForageApiResponse<String>): String {
        return vaultResponse.toString()
    }

    override fun toForageSuccessOrNull(vaultResponse: ForageApiResponse<String>): ForageApiResponse.Success<String>? {
        return if (vaultResponse is ForageApiResponse.Success) vaultResponse else null
    }

    override fun toForageErrorOrNull(vaultResponse: ForageApiResponse<String>): ForageApiResponse.Failure? {
        return if (vaultResponse is ForageApiResponse.Failure) vaultResponse else null
    }

    // Unlike VGS and Basis Theory, Vault-specific errors are handled in the try..catch block that makes
    // the request to the vault, so we just return null here.
    override fun toVaultErrorOrNull(vaultResponse: ForageApiResponse<String>): ForageApiResponse.Failure? {
        return null
    }

    /**
     * The `init` method of the SDK ensures the client is using Android M (23+) or later.
     */
    @SuppressLint("NewApi")
    @Throws(Exception::class, IllegalArgumentException::class)
    private fun buildPinTranslationParams(vaultProxyRequest: VaultProxyRequest): PinTranslationParams {
        val plainTextPan = vaultProxyRequest.params?.paymentMethod?.card?.number
            ?: throw IllegalArgumentException("PaymentMethod.card.number was null")

        val ksn = ksnFileManager!!.readAll() ?: throw IllegalArgumentException("Failed to get KSN from file")

        try {
            val plainTextPin = foragePinEditText.getTextElement().text.toString()

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

        private fun buildVaultUrl(path: String): HttpUrl =
            // this code assumes that .setForageConfig() has been called
            // on a Forage***EditText before .vaultBaseUrl gets referenced
            StopgapGlobalState.envConfig.vaultBaseUrl.toHttpUrlOrNull()!!
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
            jsonBody.put(ForageConstants.PosRequestBody.KSN, pinTranslationParams.keySerialNumber)
            jsonBody.put(ForageConstants.PosRequestBody.TXN_COUNTER, pinTranslationParams.txnCounter)

            val mediaType = "application/json".toMediaTypeOrNull()
            return jsonBody.toString().toRequestBody(mediaType)
        }
    }
}

package com.joinforage.forage.android.vault

import android.content.Context
import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.addPathSegmentsSafe
import com.joinforage.forage.android.addTrailingSlash
import com.joinforage.forage.android.core.StopgapGlobalState
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.ForageConstants
import com.joinforage.forage.android.network.NetworkService
import com.joinforage.forage.android.network.OkHttpClientBuilder
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.UnknownErrorApiResponse
import com.joinforage.forage.android.pos.encryption.storage.KsnFileManager
import com.joinforage.forage.android.ui.ForagePINEditText
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
    foragePinEditText: ForagePINEditText,
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

    private fun encryptPin(plainTextPan: String, plainTextPin: String): String {
        try {
            // does this need Context / KsnManager?
//            val dukpt = DukptService(
// //                keyRegisters = AndroidKeyStoreKeyRegisters(),
// //                deviceDerivationId = ksnManager.getDeviceDerivationId(),
// //                txCounter = ksnManager.getDukptTxCount(),
//            )
//        val (workingKey, txCount) = dukpt.generateWorkingKey()
//        return PinBlockIso4(plainTextPan, plainTextPin, workingKey).contents.toHexString().uppercase()
            return ""
        } catch (e: Exception) {
            logger.e("Failed to encrypt pin using dukpt service", e)
            throw e
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

    private fun buildPinTranslationParams(vaultProxyRequest: VaultProxyRequest): PinTranslationParams {
        val plainTextPan = vaultProxyRequest.params?.paymentMethod?.card?.number

        if (plainTextPan == null) {
            val err = "PaymentMethod.card.number was null"
            logger.e(err)
            throw IllegalArgumentException(err)
        }
        if (ksnFileManager == null) {
            val err = "ksnFileManager was null"
            logger.e(err)
            throw IllegalArgumentException(err)
        }

        val plainTextPin = foragePinEditText.getForageTextElement().text.toString()
        val encryptedPinBlock = encryptPin(plainTextPan, plainTextPin)

        val ksn = ksnFileManager!!.readAll()

//        val dukptService = DukptService(
//            ksn = ksn,
//           keyRegisters = AndroidKeyStoreKeyRegisters()
//        )

        val txnCounter = "..." // TODO!

        return PinTranslationParams(
            encryptedPinBlock = encryptedPinBlock,
            keySerialNumber = "...", // TODO!
            txnCounter = txnCounter
        )
    }

    companion object {
        // STOPGAP: global static var needed to read from the KSN file.
        internal var ksnFileManager: KsnFileManager? = null

        // this code assumes that .setForageConfig() has been called
        // on a Forage***EditText before VAULT_BASE_URL gets referenced
        private val VAULT_BASE_URL = StopgapGlobalState.envConfig.vaultBaseUrl

        private fun buildVaultUrl(path: String): HttpUrl =
            VAULT_BASE_URL.toHttpUrlOrNull()!!
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

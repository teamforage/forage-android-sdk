package com.joinforage.forage.android.ecom.services.vault.forage

import android.widget.EditText
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
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.core.services.vault.SecurePinCollector
import com.joinforage.forage.android.core.services.vault.VaultProxyRequest
import com.joinforage.forage.android.core.services.vault.VaultSubmitterParams
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

internal class RosettaPinSubmitter(
    private val editText: EditText,
    collector: SecurePinCollector,
    private val envConfig: EnvConfig,
    logger: Log,
    private val vaultUrlBuilder: ((String) -> HttpUrl) = { path -> buildVaultUrl(envConfig, path) }
) : AbstractVaultSubmitter(
    collector = collector,
    logger = logger
) {
    override val vaultType: VaultType = VaultType.FORAGE_VAULT_TYPE

    // x-key header is not applicable to forage
    override fun parseEncryptionKey(encryptionKeys: EncryptionKeys): String {
        return ""
    }

    override suspend fun submitProxyRequest(vaultProxyRequest: VaultProxyRequest): ForageApiResponse<String> {
        return try {
            val apiUrl = vaultUrlBuilder(vaultProxyRequest.path)
            val baseRequestBody = buildBaseRequestBody(vaultProxyRequest)
            val requestBody = buildForageVaultRequestBody(editText, baseRequestBody)

            val request = Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build()

            val headerValues = vaultProxyRequest.params!!

            val okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                sessionToken = headerValues.sessionToken,
                merchantId = headerValues.merchantId,
                traceId = logger.getTraceIdValue(),
                idempotencyKey = headerValues.idempotencyKey
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

    companion object {
        // this code assumes that .setForageConfig() has been called
        // on a Forage***EditText before VAULT_BASE_URL gets referenced
        private fun buildVaultUrl(envConfig: EnvConfig, path: String): HttpUrl =
            envConfig.vaultBaseUrl.toHttpUrlOrNull()!!
                .newBuilder()
                .addPathSegment("proxy")
                .addPathSegmentsSafe(path)
                .addTrailingSlash()
                .build()

        private fun buildForageVaultRequestBody(editText: EditText, baseRequestBody: Map<String, Any>): RequestBody {
            val jsonBody = JSONObject(baseRequestBody)
            jsonBody.put("pin", editText.text)

            val mediaType = "application/json".toMediaTypeOrNull()
            return jsonBody.toString().toRequestBody(mediaType)
        }
    }
}

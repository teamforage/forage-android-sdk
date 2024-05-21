package com.joinforage.forage.android.ecom.services.vault.vgs

import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeys
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.core.services.vault.SecurePinCollector
import com.joinforage.forage.android.core.services.vault.VaultProxyRequest
import com.verygoodsecurity.vgscollect.VGSCollectLogger
import com.verygoodsecurity.vgscollect.core.HTTPMethod
import com.verygoodsecurity.vgscollect.core.VGSCollect
import com.verygoodsecurity.vgscollect.core.VgsCollectResponseListener
import com.verygoodsecurity.vgscollect.core.model.network.VGSRequest
import com.verygoodsecurity.vgscollect.core.model.network.VGSResponse
import com.verygoodsecurity.vgscollect.widget.VGSEditText
import kotlin.coroutines.suspendCoroutine

internal class VgsPinSubmitter(
    private val vgsEditText: VGSEditText,
    collector: SecurePinCollector,
    private val envConfig: EnvConfig,
    logger: Log
) : AbstractVaultSubmitter(collector, logger) {
    override val vaultType: VaultType = VaultType.VGS_VAULT_TYPE
    override suspend fun submitProxyRequest(
        vaultProxyRequest: VaultProxyRequest
    ): ForageApiResponse<String> = suspendCoroutine { continuation ->
        VGSCollectLogger.isEnabled = false
        val vgsCollect = VGSCollect
            .Builder(vgsEditText.context, envConfig.vgsVaultId)
            .setEnvironment(envConfig.vgsVaultType)
            .create()
        vgsCollect.bindView(vgsEditText)

        vgsCollect.addOnResponseListeners(object : VgsCollectResponseListener {
            override fun onResponse(response: VGSResponse?) {
                // Clear all information collected before by VGSCollect
                vgsCollect.onDestroy()

                continuation.resumeWith(
                    Result.success(vaultToForageResponse(VGSResponseParser(response)))
                )
            }
        })

        val request: VGSRequest = VGSRequest.VGSRequestBuilder()
            .setMethod(HTTPMethod.POST)
            .setPath(vaultProxyRequest.path)
            .setCustomHeader(vaultProxyRequest.headers)
            .setCustomData(buildBaseRequestBody(vaultProxyRequest))
            .build()

        vgsCollect.asyncSubmit(request)
    }

    override fun parseEncryptionKey(encryptionKeys: EncryptionKeys): String {
        return encryptionKeys.vgsAlias
    }

    override fun getVaultToken(paymentMethod: PaymentMethod): String? =
        pickVaultTokenByIndex(paymentMethod, 0)
}

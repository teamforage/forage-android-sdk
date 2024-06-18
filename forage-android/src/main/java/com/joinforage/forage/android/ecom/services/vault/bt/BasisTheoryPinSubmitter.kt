package com.joinforage.forage.android.ecom.services.vault.bt

import com.basistheory.android.service.BasisTheoryElements
import com.basistheory.android.service.ProxyRequest
import com.basistheory.android.view.TextElement
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.ForageConstants
import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeys
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.core.services.vault.SecurePinCollector
import com.joinforage.forage.android.core.services.vault.VaultProxyRequest
import com.joinforage.forage.android.core.services.vault.VaultSubmitterParams

internal typealias BasisTheoryResponse = Result<Any?>

internal class BasisTheoryPinSubmitter(
    private val btTextElement: TextElement,
    collector: SecurePinCollector,
    private val envConfig: EnvConfig,
    logger: Log,
    private val buildVaultProvider: () -> BasisTheoryElements = { buildBasisTheory(envConfig) }
) : AbstractVaultSubmitter(
    collector = collector,
    logger = logger
) {
    override val vaultType: VaultType = VaultType.BT_VAULT_TYPE
    override fun parseEncryptionKey(encryptionKeys: EncryptionKeys): String = encryptionKeys.btAlias

    // Basis Theory requires a few extra headers beyond the
    // common headers to make proxy requests
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
        .setHeader(ForageConstants.Headers.BT_PROXY_KEY, envConfig.btProxyID)
        .setHeader(ForageConstants.Headers.CONTENT_TYPE, "application/json")

    override suspend fun submitProxyRequest(vaultProxyRequest: VaultProxyRequest): ForageApiResponse<String> {
        val bt = buildVaultProvider()

        val proxyRequest: ProxyRequest = ProxyRequest().apply {
            headers = vaultProxyRequest.headers
            body = ProxyRequestObject(
                pin = btTextElement,
                card_number_token = vaultProxyRequest.vaultToken
            )
            path = vaultProxyRequest.path
        }

        val vaultResponse = runCatching {
            bt.proxy.post(proxyRequest)
        }

        return vaultToForageResponse(BTResponseParser(vaultResponse))
    }

    override fun getVaultToken(paymentMethod: PaymentMethod): String? = pickVaultTokenByIndex(paymentMethod, 1)

    companion object {
        private fun buildBasisTheory(envConfig: EnvConfig): BasisTheoryElements {
            return BasisTheoryElements.builder()
                .apiKey(envConfig.btAPIKey)
                .build()
        }
    }
}

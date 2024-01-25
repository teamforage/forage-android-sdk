package com.joinforage.forage.android.mock

import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.vault.VaultSubmitter
import com.joinforage.forage.android.vault.VaultSubmitterParams

internal class MockVaultSubmitter() : VaultSubmitter {
    data class RequestContainer(
        val merchantId: String,
        val path: String,
        val paymentMethodRef: String,
        val idempotencyKey: String
    )

    private var responses =
        HashMap<String, ForageApiResponse<String>>()

    /**
     * Store the response in a map, keyed by the Vault request path.
     */
    fun setSubmitResponse(
        path: String,
        response: ForageApiResponse<String>
    ) {
        responses[path] = response
    }

    /**
     * Retrieve the response based on the request path of the VaultSubmitterParams.
     */
    override suspend fun submit(params: VaultSubmitterParams): ForageApiResponse<String> {
        return responses.getOrDefault(
            params.path,
            ForageApiResponse.Failure.fromError(
                ForageError(
                    500,
                    "unknown_server_error",
                    "Unknown Server Error"
                )
            )
        )
    }

    override fun getVaultType(): VaultType {
        return VaultType.VGS_VAULT_TYPE
    }
}

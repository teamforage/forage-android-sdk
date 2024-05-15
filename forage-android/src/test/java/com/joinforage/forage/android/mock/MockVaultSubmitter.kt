package com.joinforage.forage.android.mock

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.ForageError
import com.joinforage.forage.android.core.services.vault.VaultSubmitter
import com.joinforage.forage.android.core.services.vault.VaultSubmitterParams

internal class MockVaultSubmitter : VaultSubmitter {
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
}

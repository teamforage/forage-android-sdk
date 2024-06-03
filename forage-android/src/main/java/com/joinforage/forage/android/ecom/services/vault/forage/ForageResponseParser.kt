package com.joinforage.forage.android.ecom.services.vault.forage

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.vault.VaultResponseParser

class ForageResponseParser(rosettaResponse: ForageApiResponse<String>) : VaultResponseParser {
    override val isNullResponse: Boolean = false

    // Unlike VGS and Basis Theory, Vault-specific errors are handled in
    // the try..catch block that makes the request to the vault, so we
    // just return null here.
    override val vaultError: ForageApiResponse.Failure? = null

    override val forageError: ForageApiResponse.Failure? = if (rosettaResponse is ForageApiResponse.Failure) rosettaResponse else null
    override val successfulResponse: ForageApiResponse.Success<String>? = if (rosettaResponse is ForageApiResponse.Success) rosettaResponse else null

    override val vaultErrorMsg: String = rosettaResponse.toString()
    override val rawResponse: String = rosettaResponse.toString()
}

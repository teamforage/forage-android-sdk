package com.joinforage.forage.android.core.services.vault

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse

internal interface VaultResponseParser {
    val isNullResponse: Boolean
    val vaultError: ForageApiResponse.Failure?
    val forageError: ForageApiResponse.Failure?
    val successfulResponse: ForageApiResponse.Success<String>?

    val vaultErrorMsg: String?
    val rawResponse: String
}
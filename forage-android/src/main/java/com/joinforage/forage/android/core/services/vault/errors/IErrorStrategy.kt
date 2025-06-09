package com.joinforage.forage.android.core.services.vault.errors

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse

internal interface IErrorStrategy {
    suspend fun handleError(error: Throwable, cleanup: () -> Unit): ForageApiResponse<String>
}

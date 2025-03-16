package com.joinforage.forage.android.core.services.vault.submission

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse

/**
 * Allows storing common flow functionality (e.g. Balance Checks) within
 * a classes that can be shared across Ecom and Pos in the /core package
 * This supports composition rather than rigid inheritance.
 */
internal interface ISubmitDelegate {
    suspend fun rawSubmit(): ForageApiResponse<String>
}

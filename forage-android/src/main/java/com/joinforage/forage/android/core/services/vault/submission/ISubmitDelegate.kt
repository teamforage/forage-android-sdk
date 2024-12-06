package com.joinforage.forage.android.core.services.vault.submission

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.vault.IPmRefProvider

internal interface ISubmitDelegate {
    suspend fun submit(
        paymentMethodRefProvider: IPmRefProvider
    ): ForageApiResponse<String>
}

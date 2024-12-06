package com.joinforage.forage.android.pos.services.vault.requests

import com.joinforage.forage.android.core.services.forageapi.ClientApiRequest
import com.joinforage.forage.android.core.services.vault.VaultPaymentMethod
import com.joinforage.forage.android.pos.services.CardholderInteraction
import com.joinforage.forage.android.pos.services.vault.submission.PinTranslationParams

internal interface IPosBuildRequestDelegate {
    suspend fun buildRequest(
        paymentMethod: VaultPaymentMethod,
        idempotencyKey: String,
        traceId: String,
        pinTranslationParams: PinTranslationParams,
        interaction: CardholderInteraction
    ): ClientApiRequest
}

package com.joinforage.forage.android.pos.services.vault.requests

import com.joinforage.forage.android.core.services.forageapi.requests.ClientApiRequest
import com.joinforage.forage.android.pos.services.emvchip.CardholderInteraction
import com.joinforage.forage.android.pos.services.vault.submission.PinTranslationParams

internal interface IPosBuildRequestDelegate {
    suspend fun buildRequest(
        idempotencyKey: String,
        traceId: String,
        pinTranslationParams: PinTranslationParams,
        interaction: CardholderInteraction
    ): ClientApiRequest
}

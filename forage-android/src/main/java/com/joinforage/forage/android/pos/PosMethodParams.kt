package com.joinforage.forage.android.pos

import com.joinforage.forage.android.ui.ForageConfig
import com.joinforage.forage.android.ui.ForagePINEditText

/**
 * The [PosTokenizeCardParams] are only valid for in-store POS Terminal transactions.
 * This data class is not supported for online transactions.
 * The information encoded on Track 2 of the EBT Card’s magnetic stripe,
 * excluding the start and stop sentinels and any LRC characters.
 *
 * @property forageConfig The configuration details required to authenticate with the Forage API.
 * @param track2Data The information encoded on Track 2 of the EBT Card’s magnetic stripe,
 * excluding the start and stop sentinels and any LRC characters.
 * @param reusable Optional. Indicates whether the tokenized card can be
 * reused for multiple transactions. Defaults to true.
 */
data class PosTokenizeCardParams(
    val forageConfig: ForageConfig,
    val track2Data: String,
    val reusable: Boolean = true
)

/**
 * TODO: comments
 */
data class RefundPaymentParams(
    val foragePinEditText: ForagePINEditText,
    val paymentRef: String,
    val amount: Float,
    val reason: String,
    val metadata: Map<String, String>? = null
)

package com.joinforage.forage.android.ecom.services.vault.bt

import com.basistheory.android.view.TextElement

/**
 * Body of the proxy request to Basis Theory
 * IMPORTANT: Any changes to the [ProxyRequestObject] must be reflected in the consumer-rules.pro file
 */
internal data class ProxyRequestObject(val pin: TextElement, val card_number_token: String)
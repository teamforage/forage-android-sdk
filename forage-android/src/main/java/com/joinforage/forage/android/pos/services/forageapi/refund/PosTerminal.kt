package com.joinforage.forage.android.pos.services.forageapi.refund

import org.json.JSONObject

/**
 * @property terminalId The unique identifier for the terminal.
 * @property providerTerminalId The unique identifier for the terminal
 * in the provider system.
 */
data class PosTerminal(
    val terminalId: String,
    val providerTerminalId: String
) {
    internal constructor(jsonObject: JSONObject) : this(
        terminalId = jsonObject.getString("terminal_id"),
        providerTerminalId = jsonObject.getString("provider_terminal_id")
    )
}

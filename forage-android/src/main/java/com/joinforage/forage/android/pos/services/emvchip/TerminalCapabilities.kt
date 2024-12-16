package com.joinforage.forage.android.pos.services.emvchip

enum class TerminalCapabilities(val value: String) {
    KeyEntryOnly("key_entry_only"),
    MagSwipeOnly("magswipe_only"),
    InsertOnly("insert_only"),
    TapAndInsert("tap_and_insert")
}

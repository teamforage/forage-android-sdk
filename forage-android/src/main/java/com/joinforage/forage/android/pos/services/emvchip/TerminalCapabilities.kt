package com.joinforage.forage.android.pos.services.emvchip

/**
 * Describes the capabilities of the terminal for processing card interactions.
 *
 * This enum defines the types of card interaction methods supported by a terminal, * such as manual key entry, magnetic swipe, insert only, or tap and insert. * It provides a way to capture demographic-like data about the terminal's capabilities,
 * which may be required for reporting to state processors.
 *
 * @property value The string representation of the terminal capability.
 */
enum class TerminalCapabilities(val value: String) {
    /** This terminal supports only manual key entry. */
    KeyEntryOnly("key_entry_only"),

    /** This terminal supports only magnetic swipe card interactions. */
    MagSwipeOnly("magswipe_only"),

    /** This terminal supports only card insertion interactions. */
    InsertOnly("insert_only"),

    /** This terminal supports either card tapping or card insertion interactions. */
    TapAndInsert("tap_and_insert")
}

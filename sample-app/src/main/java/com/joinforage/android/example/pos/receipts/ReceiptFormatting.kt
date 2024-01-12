package com.joinforage.android.example.pos.receipts

/**
 * A class to capture the different ways that text on
 * a physical or digital receipt can be formatted.
 *
 * NOTE: that this excludes alignment as alignment by design
 */
data class ReceiptFormatting(
    val textSize: Int = 24,
    val lineSpace: Int = 1,
    val isUnderLine: Boolean = false,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isStrikeThruText: Boolean = false
)

package com.joinforage.android.example.pos.receipts.primitives

import com.joinforage.android.example.pos.receipts.ReceiptFormatting

/**
 * an internal representation of the different
 * text alignments we support
 */
internal enum class LinePartAlignment {
    LEFT, CENTER, RIGHT
}

/**
 * @param content - the text value of this ReceiptLinePart
 * @param alignment - the horizontal alignment of the text
 *  within the part
 * @param format - the text formatting to be applied to this
 *  specific part (e.g. bold, italic, underline, etc)
 * @param colWeight - the relative weight of this ReceiptLinePart
 *  compared to other ReceiptLinePart within the same
 *  ReceiptLayoutLine. For example if there were two parts in
 *  a line and the left part had weight of 1.5 and the right part
 *  had a weight of 0.5. Then the left part would be 3x the width
 *  and the right part would be 1x the width. This (colWeight)
 *  along with alignment are the two mechanism available for
 *  hacking together the preferred horizontal layout of text
 */
internal class ReceiptLinePart(
    internal val content: String,
    internal val alignment: LinePartAlignment,
    internal val format: ReceiptFormatting,
    internal val colWeight: Float = 1f
) {
    // these are a bunch of helper factory methods that should make
    // using ReceiptLineParts in practice easier since you only need
    // to really specify content and choose an alignment factory
    companion object {
        fun left(content: String, format: ReceiptFormatting, colWeight: Float) =
            ReceiptLinePart(content, LinePartAlignment.LEFT, format, colWeight)
        fun left(content: String, format: ReceiptFormatting) =
            ReceiptLinePart(content, LinePartAlignment.LEFT, format)
        fun center(content: String, format: ReceiptFormatting, colWeight: Float) =
            ReceiptLinePart(content, LinePartAlignment.CENTER, format, colWeight)
        fun center(content: String, format: ReceiptFormatting) =
            ReceiptLinePart(content, LinePartAlignment.CENTER, format)
        fun right(content: String, format: ReceiptFormatting, colWeight: Float) =
            ReceiptLinePart(content, LinePartAlignment.RIGHT, format, colWeight)
        fun right(content: String, format: ReceiptFormatting) =
            ReceiptLinePart(content, LinePartAlignment.RIGHT, format)
    }
}

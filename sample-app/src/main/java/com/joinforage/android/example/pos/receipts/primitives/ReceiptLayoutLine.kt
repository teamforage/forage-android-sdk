package com.joinforage.android.example.pos.receipts.primitives

import com.joinforage.android.example.pos.receipts.ReceiptFormatting

/**
 * @param parts - the list of ReceiptLineParts that should *  occupy this line. You can have as many as you want but
 *  a physical receipt is only so wide so in practice there
 *  probably shouldn't be more than 4. You can think of it
 *  like have 1 column per ReceiptLinePart in the list
 */
internal class ReceiptLayoutLine(internal vararg val parts: ReceiptLinePart) {

    // a bunch of factory methods that should make assembling
    // the layout of an actual receipt easier. These are supposed
    // to serve as lego pieces that you can stack together to
    // create actually useful receipts
    companion object {
        internal fun lineBreak(
            format: ReceiptFormatting = ReceiptFormatting()
        ) = ReceiptLayoutLine(
            ReceiptLinePart.left("", format)
        )

        internal fun singleColLeft(
            col1: String,
            format: ReceiptFormatting = ReceiptFormatting()
        ) = ReceiptLayoutLine(
            ReceiptLinePart.left(col1, format)
        )

        internal fun singleColCenter(
            col1: String,
            format: ReceiptFormatting = ReceiptFormatting()
        ) = ReceiptLayoutLine(
            ReceiptLinePart.center(col1, format)
        )

        internal fun singleColRight(
            col1: String,
            format: ReceiptFormatting = ReceiptFormatting()
        ) = ReceiptLayoutLine(
            ReceiptLinePart.right(col1, format)
        )

        internal fun doubleColLeft(
            col1: String,
            col2: String,
            format: ReceiptFormatting = ReceiptFormatting()
        ) = ReceiptLayoutLine(
            ReceiptLinePart.left(col1, format, 1f),
            ReceiptLinePart.left(col2, format, 1f)
        )

        internal fun doubleColRight(
            col1: String,
            col2: String,
            format: ReceiptFormatting = ReceiptFormatting()
        ) = ReceiptLayoutLine(
            ReceiptLinePart.right(col1, format, 1f),
            ReceiptLinePart.right(col2, format, 1f)
        )

        internal fun doubleColCenter(
            col1: String,
            col2: String,
            format: ReceiptFormatting = ReceiptFormatting()
        ) = ReceiptLayoutLine(
            ReceiptLinePart.center(col1, format),
            ReceiptLinePart.center(col2, format)
        )

        internal fun tripleCol(
            col1: String,
            col2: String,
            col3: String,
            format: ReceiptFormatting = ReceiptFormatting()
        ) = ReceiptLayoutLine(
            ReceiptLinePart.left(col1, format),
            ReceiptLinePart.center(col2, format),
            ReceiptLinePart.center(col3, format)
        )
    }
}

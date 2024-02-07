package com.joinforage.android.example.pos.receipts.primitives

import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod
import java.text.SimpleDateFormat
import java.util.Locale

fun formatReceiptTimestamp(timestamp: String): String? {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    val date = inputFormat.parse(timestamp)
    val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return date?.let { outputFormat.format(it) }
}

/**
 * The high level data structure that describes the text
 * laid out on a receipt. This data structure is meant to
 * be consumed any code that cares about. In practice,
 * the ReceiptPrinter service consumes ReceiptLayout
 * instance and translates it into the data structures that
 * the CPay SDK cares about. And, the ReceiptViewer consumes
 * a ReceiptLayout and displays a LinearLayout representation
 * of the receipt.
 *
 * Ultimately a ReceiptLayout is a list of lines
 * (called ReceiptLayoutLines) and each line has a list of
 * parts (called ReceiptLineParts). parts are what is
 * ultimately formatted, aligned, and contains text.
 */
internal open class ReceiptLayout(
    internal vararg val lines: ReceiptLayoutLine
) {
    companion object {
        fun forError(msg: String) = ReceiptLayout(
            ReceiptLayoutLine.singleColCenter(msg)
        )

        fun bottomPadding() = ReceiptLayout(
            ReceiptLayoutLine.lineBreak(),
            ReceiptLayoutLine.lineBreak(),
            ReceiptLayoutLine.lineBreak()
        )

        private fun forShortAddressMerchant(
            name: String,
            line1: String,
            cityStateZip: String,
            merchTermId: String,
            clerkId: String
        ) = ReceiptLayout(
            ReceiptLayoutLine.singleColCenter(name),
            ReceiptLayoutLine.singleColCenter(line1),
            ReceiptLayoutLine.singleColCenter(cityStateZip),
            ReceiptLayoutLine.singleColLeft(merchTermId),
            ReceiptLayoutLine.singleColLeft(clerkId)
        )

        private fun forLongAddressMerchant(
            name: String,
            line1: String,
            line2: String,
            cityStateZip: String,
            merchTermId: String,
            clerkId: String
        ) = ReceiptLayout(
            ReceiptLayoutLine.singleColCenter(name),
            ReceiptLayoutLine.singleColCenter(line1),
            ReceiptLayoutLine.singleColCenter(line2),
            ReceiptLayoutLine.singleColCenter(cityStateZip),
            ReceiptLayoutLine.singleColLeft(merchTermId),
            ReceiptLayoutLine.singleColLeft(clerkId)
        )

        fun forMerchant(name: String, line1: String, line2: String?, city: String, state: String, zipCode: String, merchantTerminalId: String): ReceiptLayout {
            val cityStateZip = "$city, $state, $zipCode"
            val merchTermId = "MERCH TERM ID $merchantTerminalId"
            val clerkId = "CLERK # 001"
            if (line2 != null) {
                return forLongAddressMerchant(name, line1, line2, cityStateZip, merchTermId, clerkId)
            }
            return forShortAddressMerchant(name, line1, cityStateZip, merchTermId, clerkId)
        }

        fun forMerchant(merchant: Merchant?): ReceiptLayout {
            if (merchant == null) {
                return forError("Merchant details not found.")
            }

            if (merchant.address == null) {
                return forError("Merchant is missing address.")
            }

            return forMerchant(
                merchant.name,
                merchant.address.line1,
                merchant.address.line2,
                merchant.address.city,
                merchant.address.state,
                merchant.address.zipcode,
                merchant.ref
            )
        }

        fun forTx(
            terminalId: String,
            txTimestamp: String,
            paymentMethod: PosPaymentMethod?,
            seqId: String?,
            txType: String
        ): ReceiptLayout {
            if (paymentMethod == null) {
                return forError("Missing tokenized PaymentMethod")
            }

            val card = paymentMethod.card
            val formattedTime = formatReceiptTimestamp(txTimestamp)
            return ReceiptLayout(
                ReceiptLayoutLine.singleColLeft("TERM ID $terminalId"),
                ReceiptLayoutLine.singleColLeft("SEQ # $seqId"),
                ReceiptLayoutLine.singleColLeft("$formattedTime"), // wrapped in string since its String?
                ReceiptLayoutLine.singleColLeft("CARD# XXXXXXXXX${card?.last4}"),
                ReceiptLayoutLine.singleColLeft("STATE: ${card?.state}"),
                ReceiptLayoutLine.lineBreak(),
                ReceiptLayoutLine.singleColCenter(txType)
            )
        }
    }
}

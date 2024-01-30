package com.joinforage.android.example.pos.receipts

import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.tokenize.PosBalance
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod
import java.text.SimpleDateFormat
import java.util.Locale

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

    // a bunch of factory methods to make defining FIS cert test
    // scenarios easier. There are also some static layouts for
    // for simple testing
    companion object {
        internal fun forBalanceCheck(
            merchant: Merchant?,
            paymentMethod: PosPaymentMethod?,
            terminalId: String
        ): ReceiptLayout {
            if (paymentMethod?.balance == null) {
                return errorMessageLayout("Balance not found")
            }

            val balance = paymentMethod.balance
            return ReceiptLayout(
                *getMerchantReceiptLayout(merchant).lines,
                *getTxReceiptLayout(
                    terminalId,
                    balance.updated,
                    paymentMethod,
                    balance.sequenceNumber,
                ).lines,
                *getBalanceReceiptLayout(balance).lines
            )
        }
        internal fun errorMessageLayout(msg: String) = ReceiptLayout(
            ReceiptLayoutLine.singleColCenter(msg)
        )

        // these methods will be used for building out the receipts
        // for the FIS Cert test cases. We can implement them as
        // we get to their respective scenes
        internal fun voidLastTxReceipt(): Nothing = TODO("implement me!!!")
        internal fun cordPulledReceipt(): Nothing = TODO("implement me!!!")
        internal fun cashWithdrawalOnlyReceipt(): Nothing = TODO("implement me!!!")
        internal fun cashPaymentOnlyReceipt(): Nothing = TODO("implement me!!!")
        internal fun cashPaymentAndWithdrawalReceipt(): Nothing = TODO("implement me!!!")
        internal fun snapPaymentReceipt(): Nothing = TODO("implement me!!!")

        // these are static receipt layouts occasionally useful for
        // developing and testing
        internal val EmptyReceiptLayout = ReceiptLayout(
            ReceiptLayoutLine.singleColCenter("This is an empty receipt.")
        )

        internal val ExampleReceipt = ReceiptLayout(
            ReceiptLayoutLine.singleColCenter("YOUR STORE NAME"),
            ReceiptLayoutLine.singleColCenter("3609 ANY STREET ADDRESS"),
            ReceiptLayoutLine.singleColCenter("YOUR TOWN, STATE ZIP CODE"),
            ReceiptLayoutLine.lineBreak(),
            ReceiptLayoutLine.doubleColLeft("TERM ID", "LA0002"),
            ReceiptLayoutLine.doubleColLeft("MERCH TERM ID", "LA0002330"),
            ReceiptLayoutLine.doubleColLeft("SEQ#", "131"),
            ReceiptLayoutLine.doubleColLeft("CLERK", "999"),
            ReceiptLayoutLine.singleColLeft("07/24/YY 08:22"),
            ReceiptLayoutLine.doubleColLeft("CARD#", "XXXXXXXXXXXX9023"),
            ReceiptLayoutLine.doubleColLeft("STATE", "LA"),
            ReceiptLayoutLine.doubleColLeft("POSTED", "07/24/YY"),
            ReceiptLayoutLine.lineBreak(),
            ReceiptLayoutLine.tripleCol("Total", "TX AMT", "END BAL"),
            ReceiptLayoutLine.tripleCol("CASH", "$3.26", "$482.00"),
            ReceiptLayoutLine.tripleCol("FS", "$0.00", "$495.00"),
            ReceiptLayoutLine.doubleColLeft("CS W/D", "$3.26 APPROVED"),
            ReceiptLayoutLine.lineBreak(),
            ReceiptLayoutLine.doubleColLeft("*** DISP CASH", "$3.26**"),
            ReceiptLayoutLine.lineBreak(),
            ReceiptLayoutLine.lineBreak(),
            ReceiptLayoutLine.lineBreak(),
            ReceiptLayoutLine.lineBreak(),
            ReceiptLayoutLine.lineBreak(),
            ReceiptLayoutLine.lineBreak(),
            ReceiptLayoutLine.lineBreak(),
            ReceiptLayoutLine.lineBreak(),
            ReceiptLayoutLine.lineBreak(),
            ReceiptLayoutLine.lineBreak(),
            ReceiptLayoutLine.lineBreak()
        )
    }
}

internal fun getShortAddressMerchantReceiptLayout(
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
internal fun getLongAddressMerchantReceiptLayout(
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

internal fun getMerchantReceiptLayout(
    name: String,
    line1: String,
    line2: String?,
    city: String,
    state: String,
    zipCode: String,
    merchantTerminalId: String
): ReceiptLayout {
    val cityStateZip = "$city, $state, $zipCode"
    val merchTermId = "MERCH TERM ID $merchantTerminalId"
    val clerkId = "CLERK # 001"
    if (line2 != null) {
        return getLongAddressMerchantReceiptLayout(
            name,
            line1,
            line2,
            cityStateZip,
            merchTermId,
            clerkId
        )
    }
    return getShortAddressMerchantReceiptLayout(
        name,
        line1,
        cityStateZip,
        merchTermId,
        clerkId
    )
}

internal fun getMerchantReceiptLayout(merchant: Merchant?): ReceiptLayout {
    if (merchant == null) {
        return ReceiptLayout.errorMessageLayout(
            "Merchant details not found."
        )
    }

    if (merchant.address == null) {
        return ReceiptLayout.errorMessageLayout(
            "Merchant is missing address."
        )
    }

    return getMerchantReceiptLayout(
        name = merchant.name,
        line1 = merchant.address.line1,
        line2 = merchant.address.line2,
        city = merchant.address.city,
        state = merchant.address.state,
        zipCode = merchant.address.zipcode,
        merchantTerminalId = merchant.ref
    )
}

fun formatReceiptTimestamp(timestamp: String): String? {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    val date = inputFormat.parse(timestamp)
    val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return date?.let { outputFormat.format(it) }
}

// TODO: remove these TODO default args once we confirm that all
//  flows for the FIS cert test have the desired values
internal fun getTxReceiptLayout(
    terminalId: String,
    txTimestamp: String,
    paymentMethod: PosPaymentMethod?,
    seqId: String?
): ReceiptLayout {
    if (paymentMethod == null) {
        return ReceiptLayout.errorMessageLayout(
            "Missing tokenized PaymentMethod"
        )
    }

    val card = paymentMethod.card
    val formattedTime = formatReceiptTimestamp(txTimestamp)
    return ReceiptLayout(
        ReceiptLayoutLine.singleColLeft("TERM ID $terminalId"),
        ReceiptLayoutLine.singleColLeft("SEQ # $seqId"),
        ReceiptLayoutLine.singleColLeft("$formattedTime"), // wrapped in string since its String?
        ReceiptLayoutLine.singleColLeft("CARD# XXXXXXXXX${card?.last4}"),
        ReceiptLayoutLine.singleColLeft("STATE: ${card?.state}")
    )
}

internal fun getBalanceReceiptLayout(balance: PosBalance): ReceiptLayout {
    return ReceiptLayout(
        ReceiptLayoutLine.doubleColCenter("SNAP BAL", balance.snap),
        ReceiptLayoutLine.doubleColCenter("EBT CASH BAL", balance.non_snap)
    )
}

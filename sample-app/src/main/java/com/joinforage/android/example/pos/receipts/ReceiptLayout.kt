package com.joinforage.android.example.pos.receipts

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
internal class ReceiptLayout(
    internal vararg val lines: ReceiptLayoutLine
) {

    // a bunch of factory methods to make defining FIS cert test
    // scenarios easier. There are also some static layouts for
    // for simple testing
    companion object {
        internal fun merchantInfoLayout(
            merchant: MerchantReceiptInfo
        ) = ReceiptLayout(
            ReceiptLayoutLine.singleColCenter(merchant.name),
            ReceiptLayoutLine.singleColCenter("${merchant.streetNumber} ${merchant.streetName}"),
            ReceiptLayoutLine.singleColCenter("${merchant.city}, ${merchant.state}, ${merchant.zipCode}")
        )

        internal fun terminalInfoLayout(
            terminal: TerminalReceiptInfo
        ) = ReceiptLayout(
            ReceiptLayoutLine.singleColLeft("TERM ID ${terminal.terminalId}"),
            ReceiptLayoutLine.singleColLeft("MERCH TERM ID ${terminal.merchantTerminalId}"),
            ReceiptLayoutLine.singleColLeft("SEQ # ${terminal.seqId}"),
            ReceiptLayoutLine.singleColLeft("CLERK # ${terminal.clerkId}"),
            ReceiptLayoutLine.singleColLeft(terminal.txTimestamp)
        )

        internal fun cardInfoLayout(
            card: CardReceiptInfo
        ) = ReceiptLayout(
            ReceiptLayoutLine.singleColLeft("CARD# XXXXXXXXX${card.last4}"),
            ReceiptLayoutLine.singleColLeft("STATE: ${card.issuingState}")
        )

        internal fun testCaseReceipt(
            merchant: MerchantReceiptInfo,
            terminal: TerminalReceiptInfo,
            card: CardReceiptInfo,
            vararg mainContent: ReceiptLayoutLine
        ) = ReceiptLayout(
            *merchantInfoLayout(merchant).lines,
            *terminalInfoLayout(terminal).lines,
            *cardInfoLayout(card).lines,
            *mainContent
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

// TODO: It's unclear if this is the most useful separation
//  of info for receipts. For example, should
//  CardReceiptInfo and TerminalReceiptInfo be
//  combined into a single TxReceiptInfo?
data class TerminalReceiptInfo(
    val terminalId: String,
    val merchantTerminalId: String,
    val seqId: String,
    val clerkId: String,
    val txTimestamp: String
)

data class CardReceiptInfo(
    val issuingState: String,
    val last4: String
)

data class MerchantReceiptInfo(
    val name: String,
    val streetNumber: String,
    val streetName: String,
    val city: String,
    val state: String,
    val zipCode: String
)

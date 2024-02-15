package com.joinforage.android.example.pos.receipts.templates.txs

import com.joinforage.android.example.pos.receipts.primitives.ReceiptLayout
import com.joinforage.android.example.pos.receipts.primitives.ReceiptLayoutLine
import com.joinforage.android.example.pos.receipts.templates.BaseReceiptTemplate
import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.Receipt
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod

fun negateAmt(amt: String) = "-$amt"

internal fun isApproved(receipt: Receipt) = receipt.message == "Approved"
internal fun getTxApprovedHeader() = ReceiptLayout(
    ReceiptLayoutLine.singleColCenter("*****APPROVED*****")
)
internal fun getTxDeclinedHeader(receipt: Receipt) = ReceiptLayout(
    ReceiptLayoutLine.singleColCenter("*****DECLINED*****"),
    ReceiptLayoutLine.singleColCenter(receipt.message)
)
internal fun getTxVoidedHeader() = ReceiptLayout(
    ReceiptLayoutLine.singleColCenter("*****VOIDED*****")
)
internal fun getTxOutcome(receipt: Receipt): ReceiptLayout {
    if (receipt.isVoided) {
        return getTxVoidedHeader()
    } else if (isApproved(receipt)) return getTxApprovedHeader()
    return getTxDeclinedHeader(receipt)
}

internal abstract class TxReceiptTemplate(
    merchant: Merchant?,
    terminalId: String,
    paymentMethod: PosPaymentMethod?,
    protected val receipt: Receipt,
    title: String
) : BaseReceiptTemplate(
    merchant,
    terminalId,
    paymentMethod
) {
    abstract val txContent: ReceiptLayout

    override val seqNumber: String = receipt.sequenceNumber
    override val timestamp: String = receipt.created
    override val snapBal: String = receipt.balance.snap
    override val cashBal: String = receipt.balance.nonSnap
    override val title = title
    override val mainContent: ReceiptLayout
        get() = ReceiptLayout(
            *getTxOutcome(receipt).lines,
            *txContent.lines
        )
}

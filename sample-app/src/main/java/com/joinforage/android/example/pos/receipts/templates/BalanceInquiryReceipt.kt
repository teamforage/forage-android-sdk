package com.joinforage.android.example.pos.receipts.templates

import com.joinforage.android.example.pos.receipts.primitives.ReceiptLayout
import com.joinforage.android.example.pos.receipts.primitives.ReceiptLayoutLine
import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod

internal fun getApprovedHeader() = ReceiptLayout(
    ReceiptLayoutLine.singleColCenter("*****APPROVED*****")
)
internal fun getDeclinedHeader(balanceCheckError: String) = ReceiptLayout(
    ReceiptLayoutLine.singleColCenter("*****DECLINED*****"),
    ReceiptLayoutLine.singleColCenter(balanceCheckError)
)
internal fun getHeader(balanceCheckError: String?) =
    if (balanceCheckError.isNullOrEmpty()) {
        getApprovedHeader()
    } else {
        getDeclinedHeader(balanceCheckError)
    }

internal class BalanceInquiryReceipt(
    merchant: Merchant?,
    terminalId: String,
    paymentMethod: PosPaymentMethod?,
    balanceCheckError: String?
) : BaseReceiptTemplate(merchant, terminalId, paymentMethod) {
    private val _balance = paymentMethod?.balance
    override val timestamp = _balance?.updated.toString()
    override val seqNumber: String = _balance?.sequenceNumber.toString()
    override val cashBal: String = _balance?.non_snap!!
    override val snapBal: String = _balance?.snap!!
    override val title = "BALANCE INQUIRY"
    override val mainContent = ReceiptLayout(
        *getHeader(balanceCheckError).lines,
        ReceiptLayoutLine.doubleColCenter("SNAP BAL", snapBal),
        ReceiptLayoutLine.doubleColCenter("EBT CASH BAL", cashBal)
    )
}

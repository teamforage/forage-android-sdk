package com.joinforage.android.example.pos.receipts.templates.txs

import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.Receipt
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod

internal class CashPurchaseWithCashbackTxReceipt : TxReceiptTemplate {
    private var cashAmt: String = receipt.ebtCashAmount
    private val cashBackAmt: String = receipt.cashBackAmount
    constructor(
        merchant: Merchant?,
        terminalId: String,
        paymentMethod: PosPaymentMethod?,
        receipt: Receipt,
        title: String
    ) : super(merchant, terminalId, paymentMethod, receipt, title) {
        cashAmt = if (isRefund(receipt)) negateAmt(cashAmt) else cashAmt
    }

    override val txContent = CashPurchaseWithCashbackLayout(
        snapBal,
        cashBal,
        cashAmt,
        cashBackAmt
    ).getLayout()
}

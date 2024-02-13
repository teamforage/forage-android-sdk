package com.joinforage.android.example.pos.receipts.templates.txs

import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.Receipt
import com.joinforage.android.example.ui.pos.data.Refund
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod

internal class CashPurchaseWithCashbackTxReceipt : TxReceiptTemplate {
    private var cashAmt: String = receipt.ebtCashAmount
    private val cashBackAmt: String = receipt.cashBackAmount
    constructor(
        merchant: Merchant?,
        terminalId: String,
        paymentMethod: PosPaymentMethod?,
        receipt: Receipt
    ) : super(merchant, terminalId, paymentMethod, receipt)

    constructor(
        merchant: Merchant?,
        terminalId: String,
        paymentMethod: PosPaymentMethod?,
        refund: Refund
    ) : super(merchant, terminalId, paymentMethod, refund.receipt!!) {
        cashAmt = negateAmt(cashAmt)
    }

    override val txContent = CashPurchaseWithCashbackLayout(
        snapBal,
        cashBal,
        cashAmt,
        cashBackAmt
    ).getLayout()
}

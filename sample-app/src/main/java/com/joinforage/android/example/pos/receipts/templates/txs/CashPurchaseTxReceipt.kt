package com.joinforage.android.example.pos.receipts.templates.txs

import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.Receipt
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod

internal class CashPurchaseTxReceipt : TxReceiptTemplate {
    private var cashAmt: String = receipt.ebtCashAmount
    constructor(
        merchant: Merchant?,
        terminalId: String,
        paymentMethod: PosPaymentMethod?,
        receipt: Receipt
    ) : super(merchant, terminalId, paymentMethod, receipt) {
        cashAmt = if (isRefund(receipt)) negateAmt(cashAmt) else cashAmt
    }

    override val txContent = CashPaymentLayout(
        snapBal,
        cashBal,
        cashAmt
    ).getLayout()
}

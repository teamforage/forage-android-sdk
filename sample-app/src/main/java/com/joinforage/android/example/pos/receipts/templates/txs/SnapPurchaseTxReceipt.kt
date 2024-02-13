package com.joinforage.android.example.pos.receipts.templates.txs

import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.Receipt
import com.joinforage.android.example.ui.pos.data.Refund
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod

internal class SnapPurchaseTxReceipt : TxReceiptTemplate {
    private var snapAmt = receipt.snapAmount
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
        snapAmt = negateAmt(snapAmt)
    }

    override val txContent = SnapPaymentLayout(
        snapBal,
        cashBal,
        snapAmt
    ).getLayout()
}

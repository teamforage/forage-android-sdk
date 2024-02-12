package com.joinforage.android.example.pos.receipts.templates.txs

import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.PosPaymentResponse
import com.joinforage.android.example.ui.pos.data.Refund
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod

internal class CashWithdrawalTxReceipt : TxReceiptTemplate {
    constructor(
        merchant: Merchant?,
        terminalId: String,
        paymentMethod: PosPaymentMethod?,
        payment: PosPaymentResponse
    ) : super(merchant, terminalId, paymentMethod, payment.receipt!!)

    // TODO: find out whether it's even possible to negate a cashback tx
    constructor(
        merchant: Merchant?,
        terminalId: String,
        paymentMethod: PosPaymentMethod?,
        refund: Refund
    ) : super(merchant, terminalId, paymentMethod, refund.receipt!!)

    override val txContent = CashWithdrawalLayout(
        snapBal,
        cashBal,
        receipt.cashBackAmount
    ).getLayout()
}

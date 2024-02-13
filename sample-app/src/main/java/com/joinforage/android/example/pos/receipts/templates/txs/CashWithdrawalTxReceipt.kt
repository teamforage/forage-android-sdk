package com.joinforage.android.example.pos.receipts.templates.txs

import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.Receipt
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod

internal class CashWithdrawalTxReceipt : TxReceiptTemplate {
    // TODO: find out whether it's even possible to negate a cashback tx
    constructor(
        merchant: Merchant?,
        terminalId: String,
        paymentMethod: PosPaymentMethod?,
        receipt: Receipt
    ) : super(merchant, terminalId, paymentMethod, receipt)

    override val txContent = CashWithdrawalLayout(
        snapBal,
        cashBal,
        receipt.cashBackAmount
    ).getLayout()
}

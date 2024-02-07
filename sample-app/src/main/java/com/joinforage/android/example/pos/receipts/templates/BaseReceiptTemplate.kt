package com.joinforage.android.example.pos.receipts.templates

import com.joinforage.android.example.pos.receipts.primitives.ReceiptLayout
import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod

internal abstract class BaseReceiptTemplate(
    private val _merchant: Merchant?,
    private val _terminalId: String,
    private val _paymentMethod: PosPaymentMethod?
) {
    abstract val timestamp: String
    abstract val seqNumber: String
    abstract val title: String
    abstract val mainContent: ReceiptLayout
    abstract val snapBal: String
    abstract val cashBal: String

    open fun getReceiptLayout() = ReceiptLayout(
        *ReceiptLayout.forMerchant(_merchant).lines,
        *ReceiptLayout.forTx(
            _terminalId,
            timestamp,
            _paymentMethod,
            seqNumber,
            title
        ).lines,
        *mainContent.lines,
        *ReceiptLayout.bottomPadding().lines
    )
}

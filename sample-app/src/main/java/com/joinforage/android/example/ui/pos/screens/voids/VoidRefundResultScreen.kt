package com.joinforage.android.example.ui.pos.screens.voids

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.joinforage.android.example.pos.receipts.templates.txs.TxType
import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.Receipt
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod
import com.joinforage.android.example.ui.pos.screens.refund.RefundResultScreen

@Composable
fun VoidRefundResultScreen(
    merchant: Merchant?,
    terminalId: String,
    paymentMethod: PosPaymentMethod?,
    paymentRef: String,
    refundRef: String?,
    txType: TxType?,
    receipt: Receipt?,
    onBackButtonClicked: () -> Unit,
    onDoneButtonClicked: () -> Unit
) {
    RefundResultScreen(
        merchant,
        terminalId,
        paymentMethod,
        paymentRef,
        refundRef,
        txType,
        receipt,
        fetchedPayment = null,
        onRefundRefClicked = { _, _ -> },
        onBackButtonClicked,
        onDoneButtonClicked,
        onReloadButtonClicked = {}
    )
}

@Preview
@Composable
fun VoidRefundResultScreenPreview() {
    VoidRefundResultScreen(
        merchant = null,
        terminalId = "",
        paymentMethod = null,
        paymentRef = "",
        refundRef = "",
        txType = null,
        receipt = null,
        onBackButtonClicked = {},
        onDoneButtonClicked = {}
    )
}

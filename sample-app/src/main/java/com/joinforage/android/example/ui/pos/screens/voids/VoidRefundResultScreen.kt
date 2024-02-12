package com.joinforage.android.example.ui.pos.screens.voids

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.joinforage.android.example.pos.receipts.templates.txs.TxType
import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.Refund
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod
import com.joinforage.android.example.ui.pos.screens.refund.RefundResultScreen

@Composable
fun VoidRefundResultScreen(
    merchant: Merchant?,
    terminalId: String,
    paymentMethod: PosPaymentMethod?,
    txType: TxType?,
    refundResponse: Refund?,
    onBackButtonClicked: () -> Unit,
    onDoneButtonClicked: () -> Unit
) {
    RefundResultScreen(
        merchant,
        terminalId,
        paymentMethod,
        txType,
        refundResponse,
        onBackButtonClicked,
        onDoneButtonClicked
    )
}

@Preview
@Composable
fun VoidRefundResultScreenPreview() {
    VoidRefundResultScreen(
        merchant = null,
        terminalId = "",
        paymentMethod = null,
        txType = null,
        refundResponse = null,
        onBackButtonClicked = {},
        onDoneButtonClicked = {}
    )
}

package com.joinforage.android.example.ui.pos.screens.voids

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.joinforage.android.example.pos.receipts.templates.txs.TxType
import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.PosPaymentResponse
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod
import com.joinforage.android.example.ui.pos.screens.payment.PaymentResultScreen

@Composable
fun VoidPaymentResultScreen(
    merchant: Merchant?,
    terminalId: String,
    paymentMethod: PosPaymentMethod?,
    txType: TxType?,
    paymentResponse: PosPaymentResponse?,
    onBackButtonClicked: () -> Unit,
    onDoneButtonClicked: () -> Unit
) {
    PaymentResultScreen(
        merchant,
        terminalId,
        paymentMethod,
        txType,
        paymentResponse,
        onBackButtonClicked,
        onDoneButtonClicked
    )
}

@Preview
@Composable
fun VoidPaymentResultScreenPreview() {
    VoidPaymentResultScreen(
        merchant = null,
        terminalId = "",
        paymentMethod = null,
        txType = null,
        paymentResponse = null,
        onBackButtonClicked = {},
        onDoneButtonClicked = {}
    )
}

package com.joinforage.android.example.ui.pos.screens.voids

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.joinforage.android.example.pos.receipts.templates.txs.TxType
import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.Receipt
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod
import com.joinforage.android.example.ui.pos.screens.payment.PaymentResultScreen

@Composable
fun VoidPaymentResultScreen(
    merchant: Merchant?,
    terminalId: String,
    paymentMethod: PosPaymentMethod?,
    paymentRef: String,
    txType: TxType?,
    receipt: Receipt?,
    onBackButtonClicked: () -> Unit,
    onDoneButtonClicked: () -> Unit
) {
    PaymentResultScreen(
        merchant,
        terminalId,
        paymentMethod,
        paymentRef,
        txType,
        receipt,
        onBackButtonClicked,
        onDoneButtonClicked,
        onReloadButtonClicked = {}
    )
}

@Preview
@Composable
fun VoidPaymentResultScreenPreview() {
    VoidPaymentResultScreen(
        merchant = null,
        terminalId = "",
        paymentMethod = null,
        paymentRef = "",
        txType = null,
        receipt = null,
        onBackButtonClicked = {},
        onDoneButtonClicked = {}
    )
}

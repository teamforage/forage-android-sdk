package com.joinforage.android.example.ui.pos.screens.voids

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.PosPaymentRequest
import com.joinforage.android.example.ui.pos.data.Refund
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod
import com.joinforage.android.example.ui.pos.screens.refund.RefundResultScreen

@Composable
fun VoidRefundResultScreen(
    merchant: Merchant?,
    terminalId: String,
    paymentMethod: PosPaymentMethod?,
    paymentRequest: PosPaymentRequest?,
    refundResponse: Refund?,
    onBackButtonClicked: () -> Unit,
    onDoneButtonClicked: () -> Unit
) {
    RefundResultScreen(
        merchant,
        terminalId,
        paymentMethod,
        paymentRequest,
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
        paymentRequest = null,
        refundResponse = null,
        onBackButtonClicked = {},
        onDoneButtonClicked = {}
    )
}

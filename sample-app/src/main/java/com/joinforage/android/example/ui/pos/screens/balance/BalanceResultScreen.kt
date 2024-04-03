package com.joinforage.android.example.ui.pos.screens.balance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.joinforage.android.example.pos.receipts.templates.BalanceInquiryReceipt
import com.joinforage.android.example.ui.extensions.withTestId
import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod
import com.joinforage.android.example.ui.pos.screens.ReceiptPreviewScreen

@Composable
fun BalanceResultScreen(
    merchant: Merchant?,
    terminalId: String,
    paymentMethod: PosPaymentMethod?,
    balanceCheckError: String?,
    onBackButtonClicked: () -> Unit,
    onDoneButtonClicked: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (paymentMethod?.balance == null) {
                Text("There was a problem checking your balance.")
            } else {
                val receipt = BalanceInquiryReceipt(
                    merchant,
                    terminalId,
                    paymentMethod,
                    balanceCheckError
                )
                ReceiptPreviewScreen(receipt.getReceiptLayout())
            }
        }
        if (paymentMethod?.balance == null) {
            Button(onClick = onBackButtonClicked, modifier = Modifier.withTestId("pos_try_again_button")) {
                Text("Try Again")
            }
        } else {
            Button(onClick = onDoneButtonClicked, modifier = Modifier.withTestId("pos_done_button")) {
                Text("Done")
            }
        }
    }
}

@Preview
@Composable
fun BalanceResultScreenPreview() {
    BalanceResultScreen(
        merchant = null,
        terminalId = "",
        paymentMethod = null,
        balanceCheckError = "",
        onBackButtonClicked = {},
        onDoneButtonClicked = {}
    )
}

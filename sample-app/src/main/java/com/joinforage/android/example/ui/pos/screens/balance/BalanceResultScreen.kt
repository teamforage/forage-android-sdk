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
import com.joinforage.android.example.pos.receipts.ReceiptLayout
import com.joinforage.android.example.ui.pos.data.BalanceCheck
import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod
import com.joinforage.android.example.ui.pos.screens.ReceiptPreviewScreen

@Composable
fun BalanceResultScreen(
    balance: BalanceCheck?,
    merchant: Merchant?,
    paymentMethod: PosPaymentMethod?,
    terminalId: String,
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
            if (balance == null) {
                Text("There was a problem checking your balance.")
            } else {
                val receipt = ReceiptLayout.forBalanceCheck(
                    merchant,
                    paymentMethod,
                    terminalId
                )
                ReceiptPreviewScreen(receipt)
            }
        }
        if (balance == null) {
            Button(onClick = onBackButtonClicked) {
                Text("Try Again")
            }
        } else {
            Button(onClick = onDoneButtonClicked) {
                Text("Done")
            }
        }
    }
}

@Preview
@Composable
fun BalanceResultScreenPreview() {
    BalanceResultScreen(
        balance = BalanceCheck(snap = "10.00", cash = "20.00"),
        merchant = null,
        paymentMethod = null,
        terminalId = "",
        onBackButtonClicked = {},
        onDoneButtonClicked = {}
    )
}

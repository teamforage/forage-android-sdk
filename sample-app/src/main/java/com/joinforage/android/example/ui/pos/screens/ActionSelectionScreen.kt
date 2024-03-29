package com.joinforage.android.example.ui.pos.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.ui.ScreenWithBottomRow

@Composable
fun ActionSelectionScreen(
    merchantDetails: Merchant?,
    onBackButtonClicked: () -> Unit,
    onBalanceButtonClicked: () -> Unit,
    onPaymentButtonClicked: () -> Unit,
    onRefundButtonClicked: () -> Unit,
    onVoidButtonClicked: () -> Unit
) {
    ScreenWithBottomRow(
        mainContent = {
            Box {
                Column {
                    Text("Merchant FNS: ${merchantDetails?.fns ?: "Unknown"}")
                }
            }
            Column(
                modifier = Modifier.padding(48.dp)
            ) {
                Button(onClick = onBalanceButtonClicked, modifier = Modifier.fillMaxWidth()) {
                    Text("Balance Inquiry")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onPaymentButtonClicked, modifier = Modifier.fillMaxWidth()) {
                    Text("Create a Payment / Purchase")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRefundButtonClicked, modifier = Modifier.fillMaxWidth()) {
                    Text("Make a Refund / Return")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onVoidButtonClicked, modifier = Modifier.fillMaxWidth()) {
                    Text("Void / Reverse a Transaction")
                }
            }
        },
        bottomRowContent = {
            Button(onClick = onBackButtonClicked) {
                Text("Back")
            }
        }
    )
}

@Preview
@Composable
fun ActionSelectionScreenPreview() {
    ActionSelectionScreen(
        merchantDetails = null,
        onBackButtonClicked = {},
        onBalanceButtonClicked = {},
        onPaymentButtonClicked = {},
        onRefundButtonClicked = {}
    ) {
    }
}

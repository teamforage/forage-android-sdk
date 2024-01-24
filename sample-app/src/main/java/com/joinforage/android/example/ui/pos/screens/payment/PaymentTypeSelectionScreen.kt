package com.joinforage.android.example.ui.pos.screens.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PaymentTypeSelectionScreen(
    onSnapPurchaseClicked: () -> Unit,
    onCashPurchaseClicked: () -> Unit,
    onCashWithdrawalClicked: () -> Unit,
    onCashPurchaseCashbackClicked: () -> Unit,
    onCancelButtonClicked: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(300.dp)
                .padding(vertical = 16.dp)
        ) {
            Text("Select a transaction type", fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onSnapPurchaseClicked, modifier = Modifier.fillMaxWidth()) {
                Text("EBT SNAP Purchase")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = onCashPurchaseClicked, modifier = Modifier.fillMaxWidth()) {
                Text("EBT Cash Purchase")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = onCashWithdrawalClicked, modifier = Modifier.fillMaxWidth(), enabled = false) {
                Text("EBT Cash Withdrawal (no purchase)")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = onCashPurchaseCashbackClicked, modifier = Modifier.fillMaxWidth(), enabled = false) {
                Text("EBT Cash Purchase + Cashback")
            }
        }
        Button(onClick = onCancelButtonClicked) {
            Text("Cancel")
        }
    }
}

@Preview
@Composable
fun PaymentTypeSelectionScreenPreview() {
    PaymentTypeSelectionScreen(
        onSnapPurchaseClicked = {},
        onCashPurchaseClicked = {},
        onCashWithdrawalClicked = {},
        onCashPurchaseCashbackClicked = {},
        onCancelButtonClicked = {}
    )
}

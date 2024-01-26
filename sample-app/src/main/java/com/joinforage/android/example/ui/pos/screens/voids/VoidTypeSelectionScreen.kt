package com.joinforage.android.example.ui.pos.screens.voids

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joinforage.android.example.ui.pos.ui.ScreenWithBottomRow

@Composable
fun VoidTypeSelectionScreen(
    onPaymentButtonClicked: () -> Unit,
    onRefundButtonClicked: () -> Unit,
    onCancelButtonClicked: () -> Unit
) {
    ScreenWithBottomRow(
        mainContent = {
            Text("Select a transaction type to void", fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onPaymentButtonClicked) {
                Text("Payment / Purchase")
            }
            Button(onClick = onRefundButtonClicked) {
                Text("Refund / Return")
            }
        },
        bottomRowContent = {
            Button(onClick = onCancelButtonClicked) {
                Text("Cancel")
            }
        }
    )
}

@Preview
@Composable
fun VoidTypeSelectionScreenPreview() {
    VoidTypeSelectionScreen(
        onPaymentButtonClicked = {},
        onRefundButtonClicked = {},
        onCancelButtonClicked = {}
    )
}

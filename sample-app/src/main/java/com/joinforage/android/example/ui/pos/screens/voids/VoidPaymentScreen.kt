package com.joinforage.android.example.ui.pos.screens.voids

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joinforage.android.example.ui.pos.ui.ScreenWithBottomRow

@Composable
fun VoidPaymentScreen(
    onConfirmButtonClicked: (paymentRef: String) -> Unit,
    onCancelButtonClicked: () -> Unit
) {
    var paymentRefInput by rememberSaveable {
        mutableStateOf("")
    }

    ScreenWithBottomRow(
        mainContent = {
            Text("Enter the ref of the payment to void", fontSize = 18.sp)
            OutlinedTextField(
                value = paymentRefInput,
                onValueChange = { paymentRefInput = it },
                label = { Text("Payment ref") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                )
            )
        },
        bottomRowContent = {
            Button(onClick = onCancelButtonClicked, colors = ButtonDefaults.elevatedButtonColors()) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = { onConfirmButtonClicked(paymentRefInput) }) {
                Text("Confirm")
            }
        }
    )
}

@Preview
@Composable
fun VoidPaymentScreenPreview() {
    VoidPaymentScreen(
        onConfirmButtonClicked = {},
        onCancelButtonClicked = {}
    )
}

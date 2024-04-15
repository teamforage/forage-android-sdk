package com.joinforage.android.example.ui.pos.screens.refund

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joinforage.android.example.ui.extensions.withTestId
import com.joinforage.android.example.ui.pos.ui.ScreenWithBottomRow

@Composable
fun RefundDetailsScreen(
    onConfirmButtonClicked: (paymentRef: String, amount: Float, reason: String) -> Unit,
    onCancelButtonClicked: () -> Unit
) {
    var paymentRefInput by rememberSaveable {
        mutableStateOf("")
    }

    var refundAmountInput by rememberSaveable {
        mutableStateOf("")
    }

    var reasonInput by rememberSaveable {
        mutableStateOf("")
    }

    ScreenWithBottomRow(
        mainContent = {
            Text("Configure the details of the refund", fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = paymentRefInput,
                onValueChange = { paymentRefInput = it },
                label = { Text("Payment ref") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.withTestId("pos_refund_payment_ref_text_field")
            )
            OutlinedTextField(
                value = refundAmountInput,
                onValueChange = { refundAmountInput = it },
                label = { Text("Refund amount") },
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.withTestId("pos_refund_amount_text_field")
            )
            OutlinedTextField(
                value = reasonInput,
                onValueChange = { reasonInput = it },
                label = { Text("Refund reason") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.withTestId("pos_refund_reason_text_field")
            )
        },
        bottomRowContent = {
            Button(onClick = onCancelButtonClicked, colors = ButtonDefaults.elevatedButtonColors()) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { onConfirmButtonClicked(paymentRefInput, refundAmountInput.toFloat(), reasonInput) },
                modifier = Modifier.withTestId("pos_submit_button")
            ) {
                Text("Confirm")
            }
        }
    )
}

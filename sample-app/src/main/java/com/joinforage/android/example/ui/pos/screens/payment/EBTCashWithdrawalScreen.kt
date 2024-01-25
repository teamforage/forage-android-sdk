package com.joinforage.android.example.ui.pos.screens.payment

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joinforage.android.example.ui.pos.ui.ScreenWithBottomRow

@Composable
fun EBTCashWithdrawalScreen(
    onConfirmButtonClicked: (amount: Double) -> Unit,
    onCancelButtonClicked: () -> Unit
) {
    var ebtCashWithdrawalAmount by rememberSaveable {
        mutableStateOf("")
    }

    ScreenWithBottomRow(
        mainContent = {
            Text("EBT Cash Purchase (no cashback)", fontSize = 18.sp)
            OutlinedTextField(
                value = ebtCashWithdrawalAmount,
                onValueChange = { ebtCashWithdrawalAmount = it },
                label = { Text("Withdrawal Amount") },
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            )
        },
        bottomRowContent = {
            Button(onClick = onCancelButtonClicked, colors = ButtonDefaults.elevatedButtonColors()) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = { onConfirmButtonClicked(ebtCashWithdrawalAmount.toDouble()) }) {
                Text("Confirm")
            }
        }
    )
}

@Preview
@Composable
fun EBTCashWithdrawalScreenPreview() {
    EBTCashWithdrawalScreen(
        onConfirmButtonClicked = {},
        onCancelButtonClicked = {}
    )
}

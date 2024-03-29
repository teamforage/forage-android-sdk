package com.joinforage.android.example.ui.pos.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.joinforage.android.example.ui.extensions.withTestId
import com.joinforage.android.example.ui.pos.ui.ScreenWithBottomRow

@Composable
fun MerchantSetupScreen(
    terminalId: String,
    merchantId: String,
    sessionToken: String,
    onSaveButtonClicked: (String, String) -> Unit
) {
    var merchantIdInput by rememberSaveable {
        mutableStateOf(merchantId)
    }

    var sessionTokenInput by rememberSaveable {
        mutableStateOf(sessionToken)
    }

    ScreenWithBottomRow(
        mainContent = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Terminal ID", fontWeight = FontWeight.Bold)
                Text(terminalId)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Merchant ID", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(48.dp))
                TextField(
                    value = merchantIdInput,
                    onValueChange = { merchantIdInput = it },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.withTestId("pos_merchant_id_text_field")
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Session Token", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(16.dp))
                TextField(
                    value = sessionTokenInput,
                    onValueChange = { sessionTokenInput = it },
                    modifier = Modifier.withTestId("pos_session_token_text_field")
                )
            }
        },
        bottomRowContent = {
            Button(
                onClick = { onSaveButtonClicked(merchantIdInput, sessionTokenInput) }
            ) {
                Text(
                    "Bind POS to Merchant",
                    modifier = Modifier.withTestId("pos_bind_to_merchant_button")
                )
            }
        }
    )
}

@Preview
@Composable
fun MerchantSetupScreenPreview() {
    MerchantSetupScreen(
        terminalId = "preview terminal id",
        merchantId = "preview merchant id",
        sessionToken = "preview session token",
        onSaveButtonClicked = { _, _ -> }
    )
}

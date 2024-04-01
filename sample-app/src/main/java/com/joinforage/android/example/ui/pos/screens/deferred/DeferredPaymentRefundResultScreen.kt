package com.joinforage.android.example.ui.pos.screens.deferred

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
internal fun DeferredPaymentRefundResultScreen(
    terminalId: String,
    paymentRef: String,
    onBackButtonClicked: () -> Unit,
    onDoneButtonClicked: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    val postRequestPrompt = """
        Send a POST request to 
        /api/payments/$paymentRef/refunds/ 
        to complete the refund
    """.trimIndent()

    val docsLink = "https://docs.joinforage.app/docs/capture-ebt-payments-server-side#step-2-complete-the-refund-server-side"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Terminal ID: $terminalId")
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString(terminalId))
                }, colors = ButtonDefaults.elevatedButtonColors()) {
                    Text("Copy")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Payment Ref: $paymentRef")
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString(paymentRef))
                }, colors = ButtonDefaults.elevatedButtonColors()) {
                    Text("Copy")
                }
            }
            Spacer(modifier = Modifier.height(48.dp))

            Text(postRequestPrompt, fontFamily = FontFamily.Monospace)

            Spacer(modifier = Modifier.height(48.dp))

            Button(onClick = {
                clipboardManager.setText(AnnotatedString(docsLink))
            }, colors = ButtonDefaults.elevatedButtonColors()) {
                Text("Copy Documentation Link")
            }
        }
        Row {
            Column {
                Button(onClick = onBackButtonClicked) {
                    Text("Try Again")
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                ElevatedButton(onClick = onDoneButtonClicked) {
                    Text("Done")
                }
            }
        }
    }
}

@Preview
@Composable
fun DeferredPaymentRefundResultScreenPreview() {
    DeferredPaymentRefundResultScreen(
        terminalId = "",
        paymentRef = "",
        onBackButtonClicked = {},
        onDoneButtonClicked = {}
    )
}

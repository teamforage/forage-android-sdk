package com.joinforage.android.example.ui.pos.ui

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.joinforage.android.example.ui.extensions.withTestId

@Composable
fun PaymentRefView(paymentRef: String) {
    val clipboardManager = LocalClipboardManager.current

    Text("Payment Ref: $paymentRef", modifier = Modifier.withTestId("pos_payment_ref_text"))
    Button(onClick = {
        clipboardManager.setText(AnnotatedString(paymentRef))
    }, colors = ButtonDefaults.elevatedButtonColors()) {
        Text("Copy")
    }
}

@Preview
@Composable
fun PaymentRefViewPreview() {
    PaymentRefView("ref-1234")
}

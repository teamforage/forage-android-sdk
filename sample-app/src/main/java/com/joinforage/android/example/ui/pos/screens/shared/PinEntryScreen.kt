package com.joinforage.android.example.ui.pos.screens.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.joinforage.android.example.ui.extensions.withTestId
import com.joinforage.android.example.ui.pos.ui.ComposableForagePINEditText
import com.joinforage.android.example.ui.pos.ui.ErrorText
import com.joinforage.android.example.ui.pos.ui.ScreenWithBottomRow
import com.joinforage.forage.android.pos.PosForageConfig
import com.joinforage.forage.android.pos.ForagePINEditText

@Composable
fun PINEntryScreen(
    posForageConfig: PosForageConfig,
    paymentMethodRef: String?,
    onSubmitButtonClicked: () -> Unit,
    onBackButtonClicked: () -> Unit,
    withPinElementReference: (element: ForagePINEditText) -> Unit,
    onDeferButtonClicked: (() -> Unit)? = null,
    errorText: String? = null
) {
    ScreenWithBottomRow(
        mainContent = {
            if (paymentMethodRef != null) {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Payment Method", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Ref: $paymentMethodRef",
                            modifier = Modifier.withTestId("pos_payment_method_ref_text")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text("Enter your card PIN")
                ComposableForagePINEditText(
                    posForageConfig = posForageConfig,
                    withPinElementReference = withPinElementReference
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = onSubmitButtonClicked,
                        modifier = Modifier.withTestId("pos_submit_button")
                    ) {
                        Text("Complete Now")
                    }

                    if (onDeferButtonClicked != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onDeferButtonClicked,
                            modifier = Modifier.withTestId("pos_collect_pin_defer_button")
                        ) {
                            Text("Defer to Server")
                        }
                    }
                }
            } else {
                Text("There was an issue adding your card")
            }
            ErrorText(errorText)
        },
        bottomRowContent = {
            Button(onClick = onBackButtonClicked) {
                if (paymentMethodRef != null) {
                    Text("Back")
                } else {
                    Text("Try Again")
                }
            }
        }
    )
}

@Preview
@Composable
fun PINEntryScreenPreview() {
    PINEntryScreen(
        posForageConfig = PosForageConfig("", ""),
        paymentMethodRef = "",
        onSubmitButtonClicked = {},
        onBackButtonClicked = {},
        withPinElementReference = {}
    )
}

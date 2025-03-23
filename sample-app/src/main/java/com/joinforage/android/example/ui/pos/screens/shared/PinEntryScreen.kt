package com.joinforage.android.example.ui.pos.screens.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.joinforage.android.example.ui.pos.ui.ComposableForagePinPad
import com.joinforage.android.example.ui.pos.ui.ErrorText
import com.joinforage.android.example.ui.pos.ui.ScreenWithBottomRow
import com.joinforage.forage.android.core.ui.element.ForageVaultElement
import com.joinforage.forage.android.core.ui.element.state.ElementState

@Composable
fun PINEntryScreen(
    last4: String?,
    onSubmitButtonClicked: (() -> Unit)? = null,
    onBackButtonClicked: () -> Unit,
    withPinElementReference: (element: ForageVaultElement<ElementState>) -> Unit,
    onDeferButtonClicked: (() -> Unit)? = null,
    errorText: String? = null
) {
    ScreenWithBottomRow(
        mainContent = {
            if (last4 != null) {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Payment Method", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Last 4: $last4",
                            modifier = Modifier.withTestId("pos_payment_method_ref_text")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text("Enter your card PIN")
                ComposableForagePINEditText(
                    withPinElementReference = withPinElementReference
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (onSubmitButtonClicked != null) {
                        OutlinedButton(
                            onClick = onSubmitButtonClicked,
                            modifier = Modifier.withTestId("pos_submit_button")
                        ) {
                            Text("Complete Now")
                        }
                    } else if (onDeferButtonClicked != null) {
                        Button(
                            onClick = onDeferButtonClicked,
                            modifier = Modifier.withTestId("pos_collect_pin_defer_button")
                        ) {
                            Text("Defer to Server")
                        }
                    } else {
                        Text("TODO: No submit or deferSubmit method passed")
                    }
                }

                // HACK: we put the error Text before the PinPad (below)
                // as a quick way to get the mobile qa tests working.
                // Unfortunately, doing this pushes the '0' key off the
                // screen. Ideally, we create a scroll view or do
                // something else with the UI so that QA tests are happy
                // and PinPad is not cut off
                ErrorText(errorText)

                // Putting the PinPad before the complete button
                // pushes the complete button out of sight below
                // the screen. Rather than getting a scrollable
                // view involved, a quick workaround was to put
                // the keypad below the complete button
                ComposableForagePinPad(withPinElementReference = withPinElementReference)
            } else {
                Text("There was an issue adding your card")
            }
        },
        bottomRowContent = {
            Button(onClick = onBackButtonClicked) {
                if (last4 != null) {
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
        last4 = "",
        onSubmitButtonClicked = {},
        onBackButtonClicked = {},
        withPinElementReference = {}
    )
}

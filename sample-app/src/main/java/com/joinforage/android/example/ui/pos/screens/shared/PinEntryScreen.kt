package com.joinforage.android.example.ui.pos.screens.shared

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.joinforage.android.example.ui.pos.ui.ComposableForagePINEditText
import com.joinforage.android.example.ui.pos.ui.ErrorText
import com.joinforage.android.example.ui.pos.ui.ScreenWithBottomRow
import com.joinforage.forage.android.pos.PosForageConfig
import com.joinforage.forage.android.ui.ForagePINEditText

@Composable
fun PINEntryScreen(
    posForageConfig: PosForageConfig,
    paymentMethodRef: String?,
    onSubmitButtonClicked: () -> Unit,
    onBackButtonClicked: () -> Unit,
    withPinElementReference: (element: ForagePINEditText) -> Unit,
    errorText: String? = null
) {
    ScreenWithBottomRow(
        mainContent = {
            if (paymentMethodRef != null) {
                Text("Enter your card PIN")
                ComposableForagePINEditText(
                    posForageConfig = posForageConfig,
                    withPinElementReference = withPinElementReference
                )
                Button(onClick = onSubmitButtonClicked) {
                    Text("Submit")
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

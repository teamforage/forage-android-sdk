package com.joinforage.android.example.ui.pos.screens.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.joinforage.android.example.ui.pos.ui.ComposableForagePINEditText
import com.joinforage.android.example.ui.pos.ui.ErrorText
import com.joinforage.forage.android.ui.ForageConfig
import com.joinforage.forage.android.ui.ForagePINEditText

@Composable
fun PINEntryScreen(
    forageConfig: ForageConfig,
    paymentMethodRef: String?,
    onSubmitButtonClicked: () -> Unit,
    onBackButtonClicked: () -> Unit,
    withPinElementReference: (element: ForagePINEditText) -> Unit,
    errorText: String? = null
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (paymentMethodRef != null) {
                Text("Enter your card PIN")
                ComposableForagePINEditText(
                    forageConfig = forageConfig,
                    withPinElementReference = withPinElementReference
                )
                Button(onClick = onSubmitButtonClicked) {
                    Text("Submit")
                }
            } else {
                Text("There was an issue adding your card")
            }
            ErrorText(errorText)
        }
        Button(onClick = onBackButtonClicked) {
            if (paymentMethodRef != null) {
                Text("Back")
            } else {
                Text("Try Again")
            }
        }
    }
}

@Preview
@Composable
fun PINEntryScreenPreview() {
    PINEntryScreen(
        forageConfig = ForageConfig("", ""),
        paymentMethodRef = "",
        onSubmitButtonClicked = {},
        onBackButtonClicked = {},
        withPinElementReference = {}
    )
}

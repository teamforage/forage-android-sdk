package com.joinforage.android.example.ui.pos.screens

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
import com.joinforage.forage.android.ui.ForagePINEditText

@Composable
fun PINEntryScreen(
    merchantId: String,
    paymentMethodRef: String?,
    onSubmitButtonClicked: () -> Unit,
    onBackButtonClicked: () -> Unit,
    withPinElementReference: (element: ForagePINEditText) -> Unit
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
                    merchantId = merchantId,
                    withPinElementReference = withPinElementReference
                )
                Button(onClick = onSubmitButtonClicked) {
                    Text("Submit")
                }
            } else {
                Text("There was an issue adding your card")
            }
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
        merchantId = "",
        paymentMethodRef = "",
        onSubmitButtonClicked = {},
        onBackButtonClicked = {},
        withPinElementReference = {}
    )
}

package com.joinforage.android.example.ui.pos.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.joinforage.android.example.network.model.tokenize.PaymentMethod

@Composable
fun PINEntryScreen(tokenizedPaymentMethod: PaymentMethod?, onBackButtonClicked: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (tokenizedPaymentMethod != null) {
                Text("Enter your card PIN")
                Text("—— PIN input will go here ——")

                if (tokenizedPaymentMethod != null) {
                    Card(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column {
                            Text("—— temporary info for dev ——")
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(tokenizedPaymentMethod.toString())
                        }
                    }
                }
            } else {
                Text("There was an issue adding your card")
            }
        }
        Button(onClick = onBackButtonClicked) {
            if (tokenizedPaymentMethod != null) {
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
        tokenizedPaymentMethod = null,
        onBackButtonClicked = {}
    )
}
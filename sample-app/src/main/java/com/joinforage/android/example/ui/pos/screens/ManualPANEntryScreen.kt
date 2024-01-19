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
import com.joinforage.android.example.ui.pos.ui.ComposableForagePANEditText
import com.joinforage.forage.android.ui.ForagePANEditText

@Composable
fun ManualPANEntryScreen(
    merchantId: String,
    tokenizedCardData: String?,
    onSubmitButtonClicked: () -> Unit,
    onBackButtonClicked: () -> Unit,
    withPanElementReference: (element: ForagePANEditText) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Manually enter your card number")
            Spacer(modifier = Modifier.height(8.dp))
            ComposableForagePANEditText(
                merchantId,
                withPanElementReference = withPanElementReference
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSubmitButtonClicked) {
                Text("Submit")
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (tokenizedCardData != null) {
                Card(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column {
                        Text("—— temporary info for dev ——")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(tokenizedCardData)
                    }
                }
            }
        }
        Button(onClick = onBackButtonClicked) {
            Text("Back")
        }
    }
}

@Preview
@Composable
fun ManualPANEntryScreenPreview() {
    ManualPANEntryScreen(
        merchantId = "",
        tokenizedCardData = "",
        onSubmitButtonClicked = {},
        onBackButtonClicked = {},
        withPanElementReference = {}
    )
}

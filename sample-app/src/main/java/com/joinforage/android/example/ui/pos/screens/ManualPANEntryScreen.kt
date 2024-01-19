package com.joinforage.android.example.ui.pos.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.joinforage.android.example.ui.pos.ui.ComposableForagePANEditText

@Composable
fun ManualPANEntryScreen(
    merchantId: String,
    onSubmitButtonClicked: () -> Unit,
    onBackButtonClicked: () -> Unit
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
            ComposableForagePANEditText(merchantId)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSubmitButtonClicked) {
                Text("Submit")
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
        onSubmitButtonClicked = {},
        onBackButtonClicked = {}
    )
}

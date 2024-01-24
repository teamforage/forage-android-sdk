package com.joinforage.android.example.ui.pos.screens.shared

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
import com.joinforage.forage.android.ui.ForageConfig
import com.joinforage.forage.android.ui.ForagePANEditText

@Composable
fun ManualPANEntryScreen(
    forageConfig: ForageConfig,
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
                forageConfig,
                withPanElementReference = withPanElementReference
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSubmitButtonClicked) {
                Text("Submit")
            }
            Spacer(modifier = Modifier.height(16.dp))
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
        forageConfig = ForageConfig("", ""),
        onSubmitButtonClicked = {},
        onBackButtonClicked = {},
        withPanElementReference = {}
    )
}

package com.joinforage.android.example.ui.pos.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun BalanceInquiryScreen(
    onManualEntryButtonClicked: () -> Unit,
    onSwipeButtonClicked: () -> Unit,
    onBackButtonClicked: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxSize()
    ) {
        Column {
            Text("How do you want to read your EBT card?")
            Button(onClick = onManualEntryButtonClicked) {
                Text("Manually Enter Card Number")
            }
            Button(onClick = onSwipeButtonClicked, enabled = false) {
                Text("Swipe Card")
            }
        }
        Row {
            Button(onClick = onBackButtonClicked) {
                Text("Back")
            }
        }
    }
}

@Preview
@Composable
fun BalanceInquiryScreenPreview() {
    BalanceInquiryScreen(
        onManualEntryButtonClicked = {},
        onSwipeButtonClicked = {},
        onBackButtonClicked = {}
    )
}

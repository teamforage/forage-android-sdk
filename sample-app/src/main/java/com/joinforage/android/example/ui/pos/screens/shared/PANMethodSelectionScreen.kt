package com.joinforage.android.example.ui.pos.screens.shared

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.joinforage.android.example.ui.pos.ui.ScreenWithBottomRow

@Composable
fun PANMethodSelectionScreen(
    onManualEntryButtonClicked: () -> Unit,
    onSwipeButtonClicked: () -> Unit,
    onBackButtonClicked: () -> Unit
) {
    ScreenWithBottomRow(
        mainContent = {
            Text("How do you want to read your EBT card?")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onManualEntryButtonClicked) {
                Text("Manually Enter Card Number")
            }
            Button(onClick = onSwipeButtonClicked) {
                Text("Swipe Card")
            }
        },
        bottomRowContent = {
            Button(onClick = onBackButtonClicked) {
                Text("Back")
            }
        }
    )
}

@Preview
@Composable
fun PANMethodSelectionScreenPreview() {
    PANMethodSelectionScreen(
        onManualEntryButtonClicked = {},
        onSwipeButtonClicked = {},
        onBackButtonClicked = {}
    )
}

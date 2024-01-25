package com.joinforage.android.example.ui.pos.screens.shared

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import com.joinforage.android.example.ui.pos.ui.ScreenWithBottomRow

@Composable
fun MagSwipePANEntryScreen(
    onLaunch: () -> Unit,
    onBackButtonClicked: () -> Unit
) {
    LaunchedEffect(Unit) {
        onLaunch()
    }

    ScreenWithBottomRow(
        mainContent = {
            Text("Swipe your EBT card now...")
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
fun MagSwipePANEntryScreenPreview() {
    MagSwipePANEntryScreen(
        onLaunch = {},
        onBackButtonClicked = {}
    )
}

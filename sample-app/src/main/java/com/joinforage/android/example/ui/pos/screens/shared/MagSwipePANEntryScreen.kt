package com.joinforage.android.example.ui.pos.screens.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MagSwipePANEntryScreen(
    onLaunch: () -> Unit,
    onBackButtonClicked: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onLaunch()
    }

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Swipe your EBT card now...")
        Button(onClick = onBackButtonClicked) {
            Text("Back")
        }
    }
}

@Preview
@Composable
fun MagSwipePANEntryScreenPreview() {
    MagSwipePANEntryScreen(
        onLaunch = {},
        onBackButtonClicked = {},
    )
}

package com.joinforage.android.example.ui.pos.screens.voids

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun VoidResultScreen(
    data: String
) {
    Column {
        Text(data)
    }
}

@Preview
@Composable
fun VoidResultScreenPreview() {
    VoidResultScreen(
        data = "some data"
    )
}

package com.joinforage.android.example.ui.pos.screens.refund

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun RefundResultScreen(
    data: String
) {
    Column {
        Text(data)
    }
}

@Preview
@Composable
fun RefundResultScreenPreview() {
    RefundResultScreen(
        data = "some data"
    )
}

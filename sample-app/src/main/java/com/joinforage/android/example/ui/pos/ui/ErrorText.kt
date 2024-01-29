package com.joinforage.android.example.ui.pos.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ErrorText(text: String?) {
    if (text != null) {
        Text(text, color = MaterialTheme.colorScheme.error)
    }
}

@Preview
@Composable
fun ErrorTextPreview() {
    ErrorText("Whoops!")
}

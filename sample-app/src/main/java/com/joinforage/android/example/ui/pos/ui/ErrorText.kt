package com.joinforage.android.example.ui.pos.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.joinforage.android.example.ui.extensions.withTestId

@Composable
fun ErrorText(text: String?) {
    if (text != null) {
        Text(
            text,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.withTestId("pos_error_text")
        )
    }
}

@Preview
@Composable
fun ErrorTextPreview() {
    ErrorText("Whoops!")
}

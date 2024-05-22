package com.joinforage.android.example.ui.pos.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.joinforage.forage.android.pos.ui.element.ForagePINEditText

@Composable
fun ComposableForagePINEditText(
    withPinElementReference: (element: ForagePINEditText) -> Unit
) {
    AndroidView(
        factory = { context ->
            ForagePINEditText(context).apply {
                this.requestFocus()
                withPinElementReference(this)
            }
        }
    )
}

@Preview
@Composable
fun ComposableForagePINEditTextPreview() {
    ComposableForagePINEditText(
        withPinElementReference = {}
    )
}

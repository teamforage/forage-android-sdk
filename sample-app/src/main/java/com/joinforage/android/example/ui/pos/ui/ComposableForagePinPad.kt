package com.joinforage.android.example.ui.pos.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.joinforage.forage.android.pos.ui.element.ForagePinPad

@Composable
fun ComposableForagePinPad(
    withPinElementReference: (element: ForagePinPad) -> Unit
) {
    AndroidView(
        factory = { context ->
            ForagePinPad(context).apply {
                withPinElementReference(this)
            }
        }
    )
}

@Preview
@Composable
fun ComposableForagePinPadPreview() {
    ComposableForagePINEditText(
        withPinElementReference = {}
    )
}

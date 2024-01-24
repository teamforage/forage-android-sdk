package com.joinforage.android.example.ui.pos.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.joinforage.forage.android.ui.ForageConfig
import com.joinforage.forage.android.ui.ForagePINEditText

@Composable
fun ComposableForagePINEditText(
    forageConfig: ForageConfig,
    withPinElementReference: (element: ForagePINEditText) -> Unit
) {
    AndroidView(
        factory = { context ->
            ForagePINEditText(context).apply {
                this.setForageConfig(forageConfig)
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
        forageConfig = ForageConfig("",""),
        withPinElementReference = {}
    )
}

package com.joinforage.android.example.ui.pos.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.joinforage.forage.android.core.ui.element.ForageConfig
import com.joinforage.forage.android.pos.ui.element.ForagePANEditText

@Composable
fun ComposableForagePANEditText(
    forageConfig: ForageConfig,
    withPanElementReference: (element: ForagePANEditText) -> Unit
) {
    AndroidView(
        factory = { context ->
            ForagePANEditText(context).apply {
                this.setForageConfig(forageConfig)
                this.requestFocus()
                withPanElementReference(this)
            }
        }
    )
}

@Preview
@Composable
fun ComposableForagePANEditTextPreview() {
    ComposableForagePANEditText(
        forageConfig = ForageConfig("", ""),
        withPanElementReference = {}
    )
}

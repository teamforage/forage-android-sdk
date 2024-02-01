package com.joinforage.android.example.ui.pos.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.joinforage.forage.android.pos.PosForageConfig
import com.joinforage.forage.android.ui.ForageConfig
import com.joinforage.forage.android.ui.ForagePANEditText

@Composable
fun ComposableForagePANEditText(
    posForageConfig: PosForageConfig,
    withPanElementReference: (element: ForagePANEditText) -> Unit
) {
    AndroidView(
        factory = { context ->
            ForagePANEditText(context).apply {
                this.setPosForageConfig(posForageConfig = posForageConfig)
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
        posForageConfig = PosForageConfig("", ""),
        withPanElementReference = {}
    )
}

package com.joinforage.android.example.ui.pos.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.joinforage.android.example.ui.pos.network.AUTH_TOKEN
import com.joinforage.forage.android.ui.ForageConfig
import com.joinforage.forage.android.ui.ForagePANEditText

@Composable
fun ComposableForagePANEditText(
    merchantId: String,
    withPanElementReference: (element: ForagePANEditText) -> Unit
) {
    AndroidView(
        factory = { context ->
            ForagePANEditText(context).apply {
                this.setForageConfig(
                    ForageConfig(
                        merchantId = merchantId,
                        sessionToken = AUTH_TOKEN
                    )
                )
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
        merchantId = "",
        withPanElementReference = {}
    )
}

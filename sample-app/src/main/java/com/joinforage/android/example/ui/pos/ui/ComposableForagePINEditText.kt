package com.joinforage.android.example.ui.pos.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.joinforage.android.example.ui.pos.network.AUTH_TOKEN
import com.joinforage.forage.android.ui.ForageConfig
import com.joinforage.forage.android.ui.ForagePINEditText

@Composable
fun ComposableForagePINEditText(
    merchantId: String,
    withPinElementReference: (element: ForagePINEditText) -> Unit
) {
    AndroidView(
        factory = { context ->
            ForagePINEditText(context).apply {
                this.setForageConfig(
                    ForageConfig(
                        merchantId = merchantId,
                        sessionToken = AUTH_TOKEN
                    )
                )
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
        merchantId = "",
        withPinElementReference = {}
    )
}

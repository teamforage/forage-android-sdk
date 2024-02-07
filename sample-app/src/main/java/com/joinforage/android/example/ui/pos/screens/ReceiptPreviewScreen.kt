package com.joinforage.android.example.ui.pos.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.joinforage.android.example.pos.receipts.primitives.ReceiptLayout
import com.joinforage.android.example.ui.pos.ui.ReceiptView

@Composable
internal fun ReceiptPreviewScreen(receiptLayout: ReceiptLayout) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            ReceiptView(context).apply {
                setReceiptLayout(receiptLayout)
            }
        }
    )
}

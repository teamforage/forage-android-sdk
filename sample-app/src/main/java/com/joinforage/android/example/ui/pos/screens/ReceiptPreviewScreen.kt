package com.joinforage.android.example.ui.pos.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.joinforage.android.example.pos.receipts.ReceiptLayout
import com.joinforage.android.example.ui.pos.ReceiptView

@Composable
fun ReceiptPreviewScreen() {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            ReceiptView(context).apply {
                setReceiptLayout(ReceiptLayout.ExampleReceipt)
            }
        }
    )
}
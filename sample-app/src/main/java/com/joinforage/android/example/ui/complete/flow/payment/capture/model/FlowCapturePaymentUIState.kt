package com.joinforage.android.example.ui.complete.flow.payment.capture.model

data class FlowCapturePaymentUIState(
    val snapAmount: Long,
    val cashAmount: Long,
    val snapPaymentRef: String,
    val cashPaymentRef: String
) {
    val isCaptureSnapVisible = snapAmount != 0L
    val isCaptureCashVisible = cashAmount != 0L
}

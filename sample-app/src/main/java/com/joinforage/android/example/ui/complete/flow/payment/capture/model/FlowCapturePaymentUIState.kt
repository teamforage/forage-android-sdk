package com.joinforage.android.example.ui.complete.flow.payment.capture.model

data class FlowCapturePaymentUIState(
    val isLoading: Boolean = false,
    val snapPaymentRef: String,
    val cashPaymentRef: String,
    val snapResponse: String = "",
    val cashResponse: String = "",
    val snapResponseError: String = "",
    val cashResponseError: String = ""
) {
    val isCaptureSnapVisible = snapPaymentRef.isNotEmpty()
    val isCaptureCashVisible = cashPaymentRef.isNotEmpty()
}

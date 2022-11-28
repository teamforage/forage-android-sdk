package com.joinforage.android.example.ui.complete.flow.payment.capture.model

data class FlowCapturePaymentUIState(
    val isLoading: Boolean = false,
    val snapAmount: Long,
    val cashAmount: Long,
    val snapPaymentRef: String,
    val cashPaymentRef: String,
    val snapResponse: String = "",
    val cashResponse: String = ""
) {
    val isCaptureSnapVisible = snapPaymentRef.isNotEmpty()
    val isCaptureCashVisible = cashPaymentRef.isNotEmpty()

    val snapAmountString = snapAmount.toString()
    val cashAmountString = cashAmount.toString()
}

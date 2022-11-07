package com.joinforage.android.example.ui.complete.flow.payment.capture

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.joinforage.android.example.ui.complete.flow.payment.capture.model.FlowCapturePaymentUIState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FlowCapturePaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = FlowCapturePaymentFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val snapAmount = args.snapAmount
    private val cashAmount = args.cashAmount
    val merchantAccount = args.merchantAccount
    val bearer = args.bearer
    val paymentMethodRef = args.paymentMethodRef
    val cardToken = args.cardToken
    val snapPaymentRef = args.snapPaymentRef
    val cashPaymentRef = args.cashPaymentRef

    private val _uiState = MutableLiveData(
        FlowCapturePaymentUIState(
            snapAmount = snapAmount,
            cashAmount = cashAmount,
            snapPaymentRef = snapPaymentRef,
            cashPaymentRef = cashPaymentRef
        )
    )

    val uiState: LiveData<FlowCapturePaymentUIState> = _uiState

    private val _isLoading = MutableLiveData<Boolean>().apply {
        value = false
    }

    val isLoading: LiveData<Boolean> = _isLoading
}

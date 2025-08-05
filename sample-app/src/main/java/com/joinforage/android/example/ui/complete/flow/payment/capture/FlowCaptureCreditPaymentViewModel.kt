package com.joinforage.android.example.ui.complete.flow.payment.capture

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joinforage.android.example.data.PaymentsRepository
import com.joinforage.android.example.network.model.CaptureRequest
import com.joinforage.android.example.network.model.PaymentResponse
import com.skydoves.sandwich.ApiResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlowCaptureCreditPaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val args = FlowCaptureCreditPaymentFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val repository: PaymentsRepository = PaymentsRepository(args.bearer)

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _paymentResult = MutableLiveData<PaymentResponse?>()
    val paymentResult: LiveData<PaymentResponse?> = _paymentResult

    private val _errorResult = MutableLiveData<String?>()
    val errorResult: LiveData<String?> = _errorResult

    fun capturePayment(captureAmount: String) = viewModelScope.launch {
        _isLoading.value = true
        _errorResult.value = null

        val captureResponse = repository.capturePayment(
            args.bearer,
            args.merchantAccount,
            args.payment.ref!!,
            captureAmount,
            listOf(
                CaptureRequest.Product(
                    gtin = "00300450406026",
                    name = "TYLNL EX/S EASY SWALLW CP 24",
                    unitPrice = captureAmount,
                    quantity = "1"
                )
            )
        )

        when (captureResponse) {
            is ApiResponse.Success -> _paymentResult.value = captureResponse.data
            is ApiResponse.Failure -> _errorResult.value = captureResponse.toString()
        }

        _isLoading.value = false
    }
}

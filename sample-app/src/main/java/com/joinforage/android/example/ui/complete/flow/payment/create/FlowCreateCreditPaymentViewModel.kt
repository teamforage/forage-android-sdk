package com.joinforage.android.example.ui.complete.flow.payment.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joinforage.android.example.data.PaymentsRepository
import com.joinforage.android.example.network.model.PaymentResponse
import com.joinforage.android.example.ui.complete.flow.payment.create.FlowCreatePaymentViewModel.Companion.FAKE_ADDRESS
import com.skydoves.sandwich.ApiResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlowCreateCreditPaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val args = FlowCreateCreditPaymentFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val repository: PaymentsRepository = PaymentsRepository(args.bearer)

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _paymentResult = MutableLiveData<PaymentResponse?>()
    val paymentResult: LiveData<PaymentResponse?> = _paymentResult

    private val _errorResult = MutableLiveData<String?>()
    val errorResult: LiveData<String?> = _errorResult

    fun createPayment(authorizationAmount: String) = viewModelScope.launch {
        _isLoading.value = true
        _paymentResult.value = null
        _errorResult.value = null

        val createResponse = repository.createPayment(
            args.bearer,
            args.merchantAccount,
            amount = authorizationAmount,
            fundingType = "credit_payfac",
            paymentMethod = args.paymentMethodRef,
            description = "desc",
            deliveryAddress = FAKE_ADDRESS,
            isDelivery = false
        )

        when (createResponse) {
            is ApiResponse.Success -> {
                val authorizeResponse = repository.authorizePayment(
                    args.bearer,
                    args.merchantAccount,
                    createResponse.data.ref!!,
                    true
                )

                when (authorizeResponse) {
                    is ApiResponse.Success -> _paymentResult.value = authorizeResponse.data
                    is ApiResponse.Failure -> _errorResult.value = authorizeResponse.toString()
                }
            }
            is ApiResponse.Failure -> _errorResult.value = createResponse.toString()
        }

        _isLoading.value = false
    }
}

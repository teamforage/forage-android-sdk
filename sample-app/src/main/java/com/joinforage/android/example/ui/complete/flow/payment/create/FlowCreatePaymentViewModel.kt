package com.joinforage.android.example.ui.complete.flow.payment.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joinforage.android.example.data.PaymentsRepository
import com.joinforage.android.example.network.model.Address
import com.joinforage.android.example.network.model.PaymentResponse
import com.skydoves.sandwich.onFailure
import com.skydoves.sandwich.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlowCreatePaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = FlowCreatePaymentFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val merchantAccount = args.merchantAccount
    val bearer = args.bearer
    val paymentMethodRef = args.paymentMethodRef
    private val repository: PaymentsRepository = PaymentsRepository(bearer)

    private val _isLoading = MutableLiveData<Boolean>().apply {
        value = false
    }

    val isLoading: LiveData<Boolean> = _isLoading

    private val _snapPaymentResult = MutableLiveData<PaymentResponse?>().apply {
        value = null
    }

    val snapPaymentResult: LiveData<PaymentResponse?> = _snapPaymentResult

    private val _snapPaymentRefResult = MutableLiveData<String?>().apply {
        value = null
    }

    val snapPaymentRefResult: LiveData<String?> = _snapPaymentRefResult

    private val _ebtCashPaymentResult = MutableLiveData<PaymentResponse?>().apply {
        value = null
    }

    val ebtCashPaymentResult: LiveData<PaymentResponse?> = _ebtCashPaymentResult

    private val _ebtCashPaymentRefResult = MutableLiveData<String?>().apply {
        value = null
    }

    val ebtCashPaymentRefResult: LiveData<String?> = _ebtCashPaymentRefResult

    fun setSnapRef(paymentRef: String) = viewModelScope.launch {
        _snapPaymentRefResult.value = paymentRef
    }

    fun setEbtCashRef(paymentRef: String) = viewModelScope.launch {
        _ebtCashPaymentRefResult.value = paymentRef
    }

    fun submitSnapAmount(amount: Long) = viewModelScope.launch {
        _isLoading.value = true

        repository.createPayment(
            bearer,
            merchantAccount,
            amount = amount,
            fundingType = "ebt_snap",
            paymentMethod = args.paymentMethodRef,
            description = "desc",
            deliveryAddress = FAKE_ADDRESS,
            isDelivery = false
        ).onSuccess {
            _snapPaymentResult.value = data
            _isLoading.value = false
        }.onFailure {
            _isLoading.value = false
        }
    }

    fun submitEbtCashAmount(amount: Long) = viewModelScope.launch {
        _isLoading.value = true

        repository.createPayment(
            bearer,
            merchantAccount,
            amount = amount,
            fundingType = "ebt_cash",
            paymentMethod = args.paymentMethodRef,
            description = "desc",
            deliveryAddress = FAKE_ADDRESS,
            isDelivery = false
        ).onSuccess {
            _ebtCashPaymentResult.value = data
            _isLoading.value = false
        }.onFailure {
            _isLoading.value = false
        }
    }

    companion object {
        private val FAKE_ADDRESS = Address(
            city = "Los Angeles",
            country = "United States",
            line1 = "Street",
            line2 = "Number",
            state = "LA",
            zipcode = "12345"
        )
    }
}

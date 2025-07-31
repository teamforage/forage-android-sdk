package com.joinforage.android.example.ui.complete.flow.tokenize

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.ecom.services.ForageSDK
import com.joinforage.forage.android.ecom.services.TokenizeCreditCardParams
import com.joinforage.forage.android.ecom.ui.element.ForagePaymentSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlowTokenizeViewPaymentSheetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val TAG = FlowTokenizeViewPaymentSheetViewModel::class.java.simpleName

    val args = FlowTokenizeFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _paymentMethod = MutableLiveData<PaymentMethod?>()
    val paymentMethod: LiveData<PaymentMethod?> get() = _paymentMethod

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun onSubmit(foragePaymentSheet: ForagePaymentSheet) = viewModelScope.launch {
        _isLoading.value = true
        _paymentMethod.value = null
        _error.value = null

        try {
            val response = ForageSDK().tokenizeCreditCard(
                TokenizeCreditCardParams(
                    foragePaymentSheet = foragePaymentSheet,
                    customerId = "android-test-customer-id-2"
                )
            )

            when (response) {
                is ForageApiResponse.Success -> {
                    Log.d(TAG, "Tokenize credit card Response: ${response.data}")
                    val newPaymentMethod = response.toPaymentMethod()
                    val creditCard = newPaymentMethod.card
                    Log.d(TAG, "Credit Card: ${creditCard.last4}")
                    _paymentMethod.value = newPaymentMethod
                }
                is ForageApiResponse.Failure -> {
                    _error.value = response.error.message
                }
            }
        } catch (e: Exception) {
            _error.value = e.toString()
        }

        _isLoading.value = false
    }
}

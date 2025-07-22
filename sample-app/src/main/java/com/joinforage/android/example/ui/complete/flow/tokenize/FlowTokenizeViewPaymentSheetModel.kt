package com.joinforage.android.example.ui.complete.flow.tokenize

import android.util.Log
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
class FlowTokenizeViewPaymentSheetModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val TAG = FlowTokenizeViewPaymentSheetModel::class.java.simpleName

    val args = FlowTokenizeFragmentArgs.fromSavedStateHandle(savedStateHandle)

    val paymentMethod = MutableLiveData<PaymentMethod?>()
    val isLoading = MutableLiveData(false)
    val error = MutableLiveData<String?>()

    fun onSubmit(foragePaymentSheet: ForagePaymentSheet) = viewModelScope.launch {
        isLoading.value = true
        paymentMethod.value = null
        error.value = null

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
                    paymentMethod.value = newPaymentMethod
                }
                is ForageApiResponse.Failure -> {
                    error.value = response.error.message
                }
            }
        } catch (e: Exception) {
            error.value = e.toString()
        }

        isLoading.value = false
    }
}

package com.joinforage.android.example.ui.complete.flow.tokenize

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joinforage.forage.android.ForageSDK
import com.joinforage.forage.android.TokenizeEBTCardParams
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.ui.ForagePANEditText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlowTokenizeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val TAG = FlowTokenizeViewModel::class.java.simpleName

    private val args = FlowTokenizeFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val merchantAccount = args.merchantAccount
    val bearer = args.bearer

    private val _paymentMethod = MutableLiveData<PaymentMethod>().apply {
        value = null
    }

    val paymentMethod: LiveData<PaymentMethod> = _paymentMethod

    private val _isLoading = MutableLiveData<Boolean>().apply {
        value = false
    }

    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>().apply {
        value = null
    }

    val error: LiveData<String?> = _error

    @OptIn(ExperimentalStdlibApi::class)
    fun onSubmit(foragePanEditText: ForagePANEditText) = viewModelScope.launch {
        _isLoading.value = true

        val response = ForageSDK().tokenizeEBTCard(
            TokenizeEBTCardParams(
                foragePanEditText = foragePanEditText,
                customerId = "android-test-customer-id"
            )
        )

        when (response) {
            is ForageApiResponse.Success -> {
                Log.d(TAG, "Tokenize EBT card Response: ${response.data}")
                val result = response.toPaymentMethod()
                _paymentMethod.value = result
            }
            is ForageApiResponse.Failure -> {
                _error.value = response.errors[0].message
            }
        }

        _isLoading.value = false
    }
}

package com.joinforage.android.example.ui.complete.flow.tokenize

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joinforage.forage.android.ForageSDK
import com.joinforage.forage.android.TokenizeEBTCardParams
import com.joinforage.forage.android.network.model.Card
import com.joinforage.forage.android.network.model.EbtCard
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.PaymentMethod
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
                // We include the `as` casting and the `when` block below
                // to ensure that the models are exported as desired
                // And support the desired casting + language features
                Log.d(TAG, "EBT Card: ${result.card.last4}")

                val ebtCard = result.card as EbtCard
                Log.d(TAG, "EBT Card: ${ebtCard.last4} ${ebtCard.usState}")
                when (val card: Card = result.card) {
                    is EbtCard -> {
                        assert(card.last4.length == 4)
                        Log.d(TAG, "EBT Card: ${card.last4} ${card.usState}")
                    }
                }
                _paymentMethod.value = result
            }
            is ForageApiResponse.Failure -> {
                _error.value = response.errors[0].message
            }
        }

        _isLoading.value = false
    }
}

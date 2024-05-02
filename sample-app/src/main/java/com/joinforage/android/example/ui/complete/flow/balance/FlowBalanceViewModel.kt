package com.joinforage.android.example.ui.complete.flow.balance

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joinforage.forage.android.core.CheckBalanceParams
import com.joinforage.forage.android.ecom.services.ForageSDK
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtBalance
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.ecom.ui.ForagePINEditText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlowBalanceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val TAG = FlowBalanceViewModel::class.java.simpleName

    private val args = FlowBalanceFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val merchantAccount = args.merchantAccount
    val bearer = args.bearer
    val paymentMethodRef = args.paymentMethodRef

    private val _snap = MutableLiveData<String>().apply {
        value = ""
    }

    val snap: LiveData<String> = _snap

    private val _nonSnap = MutableLiveData<String>().apply {
        value = ""
    }

    val nonSnap: LiveData<String> = _nonSnap

    private val _error = MutableLiveData<String>().apply {
        value = ""
    }

    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>().apply {
        value = false
    }

    val isLoading: LiveData<Boolean> = _isLoading

    @OptIn(ExperimentalStdlibApi::class)
    fun checkBalance(context: Context, pinForageEditText: ForagePINEditText) =
        viewModelScope.launch {
            _isLoading.value = true

            val response = ForageSDK().checkBalance(
                CheckBalanceParams(
                    foragePinEditText = pinForageEditText,
                    paymentMethodRef = paymentMethodRef
                )
            )

            when (response) {
                is ForageApiResponse.Success -> {
                    Log.d(TAG, "Check Balance Response: ${response.data}")
                    val balance = response.toBalance()
                    if (balance is EbtBalance) {
                        _snap.value = "SNAP: ${balance.snap}"
                        _nonSnap.value = "NON SNAP: ${balance.cash}"
                        _isLoading.value = false
                    }
                }
                is ForageApiResponse.Failure -> {
                    Log.d(TAG, "Check Balance Response: ${response.errors[0].message}")

                    _isLoading.value = false
                    _error.value = response.errors[0].message
                }
            }
        }
}

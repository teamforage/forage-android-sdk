package com.joinforage.android.example.ui.complete.flow.balance

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joinforage.android.example.network.model.balance.BalanceResponse
import com.joinforage.forage.android.CheckBalanceParams
import com.joinforage.forage.android.ForageSDK
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.ui.ForagePINEditText
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.addAdapter
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

                    val moshi = Moshi.Builder()
                        .addAdapter(Rfc3339DateJsonAdapter().nullSafe())
                        .build()
                    val adapter: JsonAdapter<BalanceResponse> =
                        moshi.adapter(BalanceResponse::class.java)

                    val result = adapter.fromJson(response.data)

                    if (result != null) {
                        _snap.value = "SNAP: ${result.snap}"
                        _nonSnap.value = "NON SNAP: ${result.cash}"
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

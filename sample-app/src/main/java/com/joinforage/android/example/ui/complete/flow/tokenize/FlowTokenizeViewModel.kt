package com.joinforage.android.example.ui.complete.flow.tokenize

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.joinforage.android.example.network.model.tokenize.PaymentMethod
import com.joinforage.forage.android.ForageSDK
import com.joinforage.forage.android.network.model.Response
import com.joinforage.forage.android.network.model.ResponseListener
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FlowTokenizeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val moshi: Moshi
) : ViewModel(), ResponseListener {

    private val args = FlowTokenizeFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val merchantAccount = args.merchantAccount
    val bearer = args.bearer

    private val TAG = FlowTokenizeViewModel::class.java.simpleName

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

    fun onSubmit() {
        _isLoading.value = true

        ForageSDK.tokenizeEBTCard(
            merchantAccount = merchantAccount,
            bearerToken = bearer,
            this@FlowTokenizeViewModel
        )
    }

    override fun onResponse(response: Response?) {
        when (response) {
            is Response.SuccessResponse -> {
                val adapter: JsonAdapter<PaymentMethod> = moshi.adapter(PaymentMethod::class.java)

                val result = response.rawResponse?.let { adapter.fromJson(it) }

                Log.d(TAG, "PaymentMethod:")
                Log.d(TAG, "$result")

                _paymentMethod.postValue(result)
            }
            is Response.ErrorResponse -> {
                val body = response.body
                Log.e(TAG, "Client Error")
                Log.e(TAG, body.orEmpty())

                _error.postValue(body)
            }
            else -> {
            }
        }
        _isLoading.postValue(false)
    }
}

package com.joinforage.android.example.ui.complete.flow.balance

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.joinforage.forage.android.ForageSDK
import com.joinforage.forage.android.network.model.Response
import com.joinforage.forage.android.network.model.ResponseListener
import com.joinforage.forage.android.ui.ForagePINEditText
import dagger.hilt.android.lifecycle.HiltViewModel
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class FlowBalanceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args = FlowBalanceFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val merchantAccount = args.merchantAccount
    val bearer = args.bearer
    val paymentMethodRef = args.paymentMethodRef
    val cardToken = args.cardToken

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

    private val _isNextVisible = MutableLiveData<Boolean>().apply {
        value = false
    }

    val isNextVisible: LiveData<Boolean> = _isNextVisible

    fun checkBalance(context: Context, cardNumberField: ForagePINEditText) {
        _isLoading.value = true

        ForageSDK.checkBalance(
            context = context,
            pinForageEditText = cardNumberField,
            merchantAccount = merchantAccount,
            bearerToken = bearer,
            paymentMethodRef = paymentMethodRef,
            cardToken = cardToken,
            onResponseListener = object : ResponseListener {
                override fun onResponse(response: Response?) {
                    when (response) {
                        is Response.SuccessResponse -> {
                            println("Check Balance Response Code: \n ${response.successCode}")
                            println("Check Balance Response: \n $response")

                            val resp = response.body?.let { JSONObject(it) }
                            if (resp != null) {
                                _snap.postValue("SNAP: ${resp.getString("snap")}")
                                _nonSnap.postValue("NON SNAP: ${resp.getString("non_snap")}")
                                _isLoading.postValue(false)
                                _isNextVisible.postValue(true)
                            }
                        }
                        is Response.ErrorResponse -> {
                            _isLoading.postValue(false)
                            _error.postValue(response.toString())
                        }
                        null -> {
                            TODO("Understand why would VGS return null here")
                        }
                    }
                }
            }
        )
    }
}

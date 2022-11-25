package com.joinforage.android.example.ui.complete.flow.payment.capture

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.joinforage.android.example.ui.base.BaseViewModel
import com.joinforage.android.example.ui.complete.flow.payment.capture.model.FlowCapturePaymentUIState
import com.joinforage.forage.android.ForageSDK
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.ui.ForagePINEditText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlowCapturePaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {
    private val args = FlowCapturePaymentFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val snapAmount = args.snapAmount
    private val cashAmount = args.cashAmount
    private val merchantAccount = args.merchantAccount
    private val bearer = args.bearer
    private val paymentMethodRef = args.paymentMethodRef
    private val cardToken = args.cardToken
    private val snapPaymentRef = args.snapPaymentRef
    private val cashPaymentRef = args.cashPaymentRef

    private val _uiState = MutableLiveData(
        FlowCapturePaymentUIState(
            snapAmount = snapAmount,
            cashAmount = cashAmount,
            snapPaymentRef = snapPaymentRef,
            cashPaymentRef = cashPaymentRef
        )
    )

    val uiState: LiveData<FlowCapturePaymentUIState> = _uiState

    fun captureSnapAmount(context: Context, pinForageEditText: ForagePINEditText) =
        viewModelScope.launch {
            _uiState.value = _uiState.value!!.copy(isLoading = true)

            val response = ForageSDK.capturePayment(
                context = context,
                pinForageEditText = pinForageEditText,
                merchantAccount = merchantAccount,
                bearerToken = bearer,
                paymentRef = snapPaymentRef,
                cardToken = cardToken
            )

            when (response) {
                is ForageApiResponse.Success -> {
                    _uiState.value = _uiState.value!!.copy(
                        isLoading = false,
                        snapResponse = response.data
                    )
                }
                is ForageApiResponse.Failure -> {
                    _uiState.value = _uiState.value!!.copy(
                        isLoading = false,
                        snapResponse = response.message
                    )
                }
            }
        }

    fun captureCashAmount(context: Context, pinForageEditText: ForagePINEditText) =
        viewModelScope.launch {
            _uiState.value = _uiState.value!!.copy(isLoading = true)

            val response = ForageSDK.capturePayment(
                context = context,
                pinForageEditText = pinForageEditText,
                merchantAccount = merchantAccount,
                bearerToken = bearer,
                paymentRef = cashPaymentRef,
                cardToken = cardToken
            )

            when (response) {
                is ForageApiResponse.Success -> {
                    _uiState.value = _uiState.value!!.copy(
                        isLoading = false,
                        cashResponse = response.data
                    )
                }
                is ForageApiResponse.Failure -> {
                    _uiState.value = _uiState.value!!.copy(
                        isLoading = false,
                        cashResponse = response.message
                    )
                }
            }
        }
}

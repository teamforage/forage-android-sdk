package com.joinforage.android.example.ui.complete.flow.payment.capture

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.joinforage.android.example.ui.base.BaseViewModel
import com.joinforage.android.example.ui.complete.flow.payment.capture.model.FlowCapturePaymentUIState
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.ecom.services.CapturePaymentParams
import com.joinforage.forage.android.ecom.services.DeferPaymentCaptureParams
import com.joinforage.forage.android.ecom.services.ForageSDK
import com.joinforage.forage.android.ecom.ui.ForagePINEditText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlowCapturePaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {
    private val TAG = FlowCapturePaymentViewModel::class.java.simpleName

    private val args = FlowCapturePaymentFragmentArgs.fromSavedStateHandle(savedStateHandle)
    internal val merchantAccount = args.merchantAccount
    internal val bearer = args.bearer
    private val snapPaymentRef = args.snapPaymentRef
    private val cashPaymentRef = args.cashPaymentRef

    private val _uiState = MutableLiveData(
        FlowCapturePaymentUIState(
            snapPaymentRef = snapPaymentRef,
            cashPaymentRef = cashPaymentRef
        )
    )

    val uiState: LiveData<FlowCapturePaymentUIState> = _uiState

    fun captureSnapAmount(pinForageEditText: ForagePINEditText) =
        viewModelScope.launch {
            _uiState.value = _uiState.value!!.copy(isLoading = true)

            val response = ForageSDK().capturePayment(
                CapturePaymentParams(
                    foragePinEditText = pinForageEditText,
                    paymentRef = snapPaymentRef
                )
            )

            when (response) {
                is ForageApiResponse.Success -> {
                    Log.d(TAG, "Capture Snap Payment Response: ${response.data}")
                    val payment = response.toPayment()
                    Log.d(TAG, "Typed Payment: $payment")

                    _uiState.value = _uiState.value!!.copy(
                        isLoading = false,
                        snapResponse = response.data
                    )
                }
                is ForageApiResponse.Failure -> {
                    Log.d(TAG, "Capture Snap Payment Response: ${response.errors[0].message}")

                    _uiState.value = _uiState.value!!.copy(
                        isLoading = false,
                        snapResponse = response.errors[0].message,
                        snapResponseError = response.errors[0].toString()
                    )
                }
            }
        }

    fun deferPaymentCaptureSnap(pinForageEditText: ForagePINEditText) =
        viewModelScope.launch {
            _uiState.value = _uiState.value!!.copy(isLoading = true)

            val response = ForageSDK().deferPaymentCapture(
                DeferPaymentCaptureParams(
                    foragePinEditText = pinForageEditText,
                    paymentRef = snapPaymentRef
                )
            )

            when (response) {
                is ForageApiResponse.Success -> {
                    Log.d(TAG, "Defer Capture Snap Response: ${response.data}")

                    _uiState.value = _uiState.value!!.copy(
                        isLoading = false,
                        snapResponse = "deferPaymentCapture: success"
                    )
                }
                is ForageApiResponse.Failure -> {
                    Log.d(TAG, "Defer Capture Snap Response: ${response.errors[0].message}")

                    _uiState.value = _uiState.value!!.copy(
                        isLoading = false,
                        snapResponse = response.errors[0].message,
                        snapResponseError = response.errors[0].toString()
                    )
                }
            }
        }

    fun captureCashAmount(pinForageEditText: ForagePINEditText) =
        viewModelScope.launch {
            _uiState.value = _uiState.value!!.copy(isLoading = true)

            val response = ForageSDK().capturePayment(
                CapturePaymentParams(
                    foragePinEditText = pinForageEditText,
                    paymentRef = cashPaymentRef
                )
            )

            when (response) {
                is ForageApiResponse.Success -> {
                    Log.d(TAG, "Capture Cash Payment Response: ${response.data}")

                    val payment = response.toPayment()
                    Log.d(TAG, "Typed Payment: $payment")

                    _uiState.value = _uiState.value!!.copy(
                        isLoading = false,
                        cashResponse = response.data
                    )
                }
                is ForageApiResponse.Failure -> {
                    Log.d(TAG, "Capture Cash Payment Response: ${response.errors[0].message}")

                    _uiState.value = _uiState.value!!.copy(
                        isLoading = false,
                        cashResponse = response.errors[0].message,
                        cashResponseError = response.errors[0].toString()
                    )
                }
            }
        }

    fun deferPaymentCaptureCash(pinForageEditText: ForagePINEditText) =
        viewModelScope.launch {
            _uiState.value = _uiState.value!!.copy(isLoading = true)

            val response = ForageSDK().deferPaymentCapture(
                DeferPaymentCaptureParams(
                    foragePinEditText = pinForageEditText,
                    paymentRef = cashPaymentRef
                )
            )

            when (response) {
                is ForageApiResponse.Success -> {
                    Log.d(TAG, "Defer Capture Cash Response: ${response.data}")

                    _uiState.value = _uiState.value!!.copy(
                        isLoading = false,
                        cashResponse = "deferPaymentCapture: success"
                    )
                }
                is ForageApiResponse.Failure -> {
                    Log.d(TAG, "Defer Capture Cash Response: ${response.errors[0].message}")

                    _uiState.value = _uiState.value!!.copy(
                        isLoading = false,
                        cashResponse = response.errors[0].message,
                        cashResponseError = response.errors[0].toString()
                    )
                }
            }
        }
}

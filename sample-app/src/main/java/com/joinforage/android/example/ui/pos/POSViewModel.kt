package com.joinforage.android.example.ui.pos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joinforage.android.example.network.model.PaymentResponse
import com.joinforage.android.example.network.model.PaymentResponseJsonAdapter
import com.joinforage.android.example.network.model.tokenize.PaymentMethod
import com.joinforage.android.example.network.model.tokenize.PaymentMethodJsonAdapter
import com.joinforage.android.example.ui.pos.data.BalanceCheck
import com.joinforage.android.example.ui.pos.data.BalanceCheckJsonAdapter
import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.POSUIState
import com.joinforage.android.example.ui.pos.data.PosPaymentRequest
import com.joinforage.android.example.ui.pos.network.PosApi
import com.joinforage.android.example.ui.pos.network.formatAuthHeader
import com.joinforage.forage.android.CapturePaymentParams
import com.joinforage.forage.android.CheckBalanceParams
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.pos.ForageTerminalSDK
import com.joinforage.forage.android.pos.PosTokenizeCardParams
import com.joinforage.forage.android.ui.ForagePANEditText
import com.joinforage.forage.android.ui.ForagePINEditText
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.UUID

sealed interface MerchantDetailsState {
    object Idle : MerchantDetailsState
    data class Success(val merchant: Merchant) : MerchantDetailsState
    data class Error(val error: String) : MerchantDetailsState
    object Loading : MerchantDetailsState
}

class POSViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(POSUIState())
    val uiState: StateFlow<POSUIState> = _uiState.asStateFlow()

    fun setMerchantId(merchantId: String, onSuccess: () -> Unit) {
        _uiState.update { it.copy(merchantId = merchantId) }
        getMerchantInfo(merchantId = merchantId, onSuccess)
    }

    fun setLocalPayment(payment: PosPaymentRequest) {
        _uiState.update { it.copy(localPayment = payment) }
    }

    private fun getMerchantInfo(merchantId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(merchantDetailsState = MerchantDetailsState.Loading) }
            val merchantDetailsState = try {
                val result = PosApi.retrofitService.getMerchantInfo(
                    formatAuthHeader(uiState.value.sessionToken),
                    merchantId
                )
                onSuccess()
                MerchantDetailsState.Success(result)
            } catch (e: HttpException) {
                MerchantDetailsState.Error(e.toString())
            }
            _uiState.update { it.copy(merchantDetailsState = merchantDetailsState) }
        }
    }

    fun createPayment(merchantId: String, payment: PosPaymentRequest, onSuccess: (response: PaymentResponse) -> Unit) {
        val idempotencyKey = UUID.randomUUID().toString()

        viewModelScope.launch {
            try {
                val response = PosApi.retrofitService.createPayment(
                    authorization = formatAuthHeader(uiState.value.sessionToken),
                    merchantId = merchantId,
                    idempotencyKey = idempotencyKey,
                    payment = payment
                )
                _uiState.update { it.copy(serverPayment = response) }
                onSuccess(response)
                Log.i("POSViewModel", "Create payment call succeeded: $response")
            } catch (e: HttpException) {
                Log.e("POSViewModel", "Create payment call failed: $e")
            }
        }
    }

    fun tokenizeEBTCard(foragePanEditText: ForagePANEditText, terminalId: String, onSuccess: (data: PaymentMethod?) -> Unit) {
        viewModelScope.launch {
            val response = ForageTerminalSDK(terminalId).tokenizeCard(
                foragePanEditText = foragePanEditText,
                reusable = true
            )

            when (response) {
                is ForageApiResponse.Success -> {
                    val moshi = Moshi.Builder().build()
                    val jsonAdapter: JsonAdapter<PaymentMethod> = PaymentMethodJsonAdapter(moshi)
                    val tokenizedPaymentMethod = jsonAdapter.fromJson(response.data)
                    _uiState.update { it.copy(tokenizedPaymentMethod = tokenizedPaymentMethod) }
                    onSuccess(tokenizedPaymentMethod)
                }
                is ForageApiResponse.Failure -> {
                    Log.e("POSViewModel", response.toString())
                }
            }
        }
    }

    fun tokenizeEBTCard(track2Data: String, terminalId: String, onSuccess: (data: PaymentMethod?) -> Unit) {
        viewModelScope.launch {
            val forage = ForageTerminalSDK(terminalId)
            val response = forage.tokenizeCard(
                PosTokenizeCardParams(
                    uiState.value.forageConfig,
                    track2Data
                )
            )

            when (response) {
                is ForageApiResponse.Success -> {
                    val moshi = Moshi.Builder().build()
                    val jsonAdapter: JsonAdapter<PaymentMethod> = PaymentMethodJsonAdapter(moshi)
                    val tokenizedPaymentMethod = jsonAdapter.fromJson(response.data)
                    _uiState.update { it.copy(tokenizedPaymentMethod = tokenizedPaymentMethod) }
                    onSuccess(tokenizedPaymentMethod)
                }
                is ForageApiResponse.Failure -> {
                    Log.e("POSViewModel", response.toString())
                }
            }
        }
    }

    fun checkEBTCardBalance(foragePinEditText: ForagePINEditText, paymentMethodRef: String, terminalId: String, onSuccess: (response: BalanceCheck?) -> Unit) {
        viewModelScope.launch {
            val response = ForageTerminalSDK(terminalId).checkBalance(
                CheckBalanceParams(
                    foragePinEditText = foragePinEditText,
                    paymentMethodRef = paymentMethodRef
                )
            )

            when (response) {
                is ForageApiResponse.Success -> {
                    val moshi = Moshi.Builder().build()
                    val jsonAdapter: JsonAdapter<BalanceCheck> = BalanceCheckJsonAdapter(moshi)
                    val balance = jsonAdapter.fromJson(response.data)
                    _uiState.update { it.copy(balance = balance) }
                    onSuccess(balance)
                }
                is ForageApiResponse.Failure -> {
                    Log.e("POSViewModel", response.toString())
                }
            }
        }
    }

    fun capturePayment(foragePinEditText: ForagePINEditText, terminalId: String, paymentRef: String, onSuccess: (response: PaymentResponse?) -> Unit) {
        viewModelScope.launch {
            val response = ForageTerminalSDK(terminalId).capturePayment(
                CapturePaymentParams(
                    foragePinEditText = foragePinEditText,
                    paymentRef = paymentRef
                )
            )

            when (response) {
                is ForageApiResponse.Success -> {
                    val moshi = Moshi.Builder().build()
                    val jsonAdapter: JsonAdapter<PaymentResponse> = PaymentResponseJsonAdapter(moshi)
                    val paymentResponse = jsonAdapter.fromJson(response.data)
                    _uiState.update { it.copy(paymentResponse = paymentResponse) }
                    onSuccess(paymentResponse)
                }
                is ForageApiResponse.Failure -> {
                    Log.e("POSViewModel", response.toString())
                }
            }
        }
    }
}

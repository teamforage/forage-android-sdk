package com.joinforage.android.example.ui.pos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joinforage.android.example.ui.pos.data.BalanceCheck
import com.joinforage.android.example.ui.pos.data.BalanceCheckJsonAdapter
import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.POSUIState
import com.joinforage.android.example.ui.pos.data.PosPaymentRequest
import com.joinforage.android.example.ui.pos.data.PosPaymentResponse
import com.joinforage.android.example.ui.pos.data.PosPaymentResponseJsonAdapter
import com.joinforage.android.example.ui.pos.data.Refund
import com.joinforage.android.example.ui.pos.data.RefundJsonAdapter
import com.joinforage.android.example.ui.pos.data.RefundUIState
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethodJsonAdapter
import com.joinforage.android.example.ui.pos.network.PosApiService
import com.joinforage.forage.android.CapturePaymentParams
import com.joinforage.forage.android.CheckBalanceParams
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.pos.ForageTerminalSDK
import com.joinforage.forage.android.pos.PosRefundPaymentParams
import com.joinforage.forage.android.pos.PosTokenizeCardParams
import com.joinforage.forage.android.ui.ForagePANEditText
import com.joinforage.forage.android.ui.ForagePINEditText
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.delay
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
    private val api
        get() = PosApiService.from(uiState.value.posForageConfig)

    fun setMerchantId(merchantId: String, onSuccess: () -> Unit) {
        _uiState.update { it.copy(merchantId = merchantId) }
        getMerchantInfo(onSuccess)
    }

    fun setLocalPayment(payment: PosPaymentRequest) {
        _uiState.update { it.copy(localPayment = payment) }
    }

    fun setLocalRefundState(refundState: RefundUIState) {
        _uiState.update { it.copy(localRefundState = refundState) }
    }

    fun resetUiState() {
        // this needs to be in a coroutine and delayed to allow for the back-stack
        // to be fully popped before some of the data it depends on disappears.
        // There's probably a better way to do this but this works for now.
        viewModelScope.launch {
            delay(1000)
            _uiState.update {
                POSUIState(
                    merchantId = it.merchantId,
                    sessionToken = it.sessionToken,
                    merchantDetailsState = it.merchantDetailsState
                )
            }
        }
    }

    private fun getMerchantInfo(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(merchantDetailsState = MerchantDetailsState.Loading) }
            val merchantDetailsState = try {
                val result = api.getMerchantInfo()
                onSuccess()
                MerchantDetailsState.Success(result)
            } catch (e: HttpException) {
                MerchantDetailsState.Error(e.toString())
            }
            _uiState.update { it.copy(merchantDetailsState = merchantDetailsState) }
        }
    }

    fun createPayment(payment: PosPaymentRequest, onSuccess: (response: PosPaymentResponse) -> Unit) {
        val idempotencyKey = UUID.randomUUID().toString()

        viewModelScope.launch {
            try {
                val response = api.createPayment(
                    idempotencyKey = idempotencyKey,
                    payment = payment
                )
                _uiState.update { it.copy(createPaymentResponse = response, createPaymentError = null) }
                onSuccess(response)
            } catch (e: HttpException) {
                Log.e("POSViewModel", "Create payment call failed: $e")
                _uiState.update { it.copy(createPaymentError = e.toString(), createPaymentResponse = null) }
            }
        }
    }

    fun tokenizeEBTCard(foragePanEditText: ForagePANEditText, terminalId: String, onSuccess: (data: PosPaymentMethod?) -> Unit) {
        viewModelScope.launch {
            val response = ForageTerminalSDK(terminalId).tokenizeCard(
                foragePanEditText = foragePanEditText,
                reusable = true
            )

            when (response) {
                is ForageApiResponse.Success -> {
                    val moshi = Moshi.Builder().build()
                    val jsonAdapter: JsonAdapter<PosPaymentMethod> = PosPaymentMethodJsonAdapter(moshi)
                    val tokenizedPaymentMethod = jsonAdapter.fromJson(response.data)
                    _uiState.update { it.copy(tokenizedPaymentMethod = tokenizedPaymentMethod, tokenizationError = null) }
                    onSuccess(tokenizedPaymentMethod)
                }
                is ForageApiResponse.Failure -> {
                    Log.e("POSViewModel", response.toString())
                    _uiState.update { it.copy(tokenizationError = response.toString(), tokenizedPaymentMethod = null) }
                }
            }
        }
    }

    fun tokenizeEBTCard(track2Data: String, terminalId: String, onSuccess: (data: PosPaymentMethod?) -> Unit) {
        viewModelScope.launch {
            val forage = ForageTerminalSDK(terminalId)
            val response = forage.tokenizeCard(
                PosTokenizeCardParams(
                    uiState.value.posForageConfig,
                    track2Data
                )
            )

            when (response) {
                is ForageApiResponse.Success -> {
                    val moshi = Moshi.Builder().build()
                    val jsonAdapter: JsonAdapter<PosPaymentMethod> = PosPaymentMethodJsonAdapter(moshi)
                    val tokenizedPaymentMethod = jsonAdapter.fromJson(response.data)
                    _uiState.update { it.copy(tokenizedPaymentMethod = tokenizedPaymentMethod, tokenizationError = null) }
                    onSuccess(tokenizedPaymentMethod)
                }
                is ForageApiResponse.Failure -> {
                    Log.e("POSViewModel", response.toString())
                    _uiState.update { it.copy(tokenizationError = response.toString(), tokenizedPaymentMethod = null) }
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
                    _uiState.update { it.copy(balance = balance, balanceCheckError = null) }
                    onSuccess(balance)

                    // we need to refetch the EBT Card here because
                    // `ForageTerminalSDK(terminalId).checkBalance`
                    // does not return the timestamp of the balance
                    // check and we need to display the timestamp
                    // on the receipt
                    val updatedCard = api.getPaymentMethod(paymentMethodRef)
                    _uiState.update {
                        it.copy(tokenizedPaymentMethod = updatedCard)
                    }
                }
                is ForageApiResponse.Failure -> {
                    Log.e("POSViewModel", response.toString())
                    _uiState.update { it.copy(balanceCheckError = response.toString(), balance = null) }
                }
            }
        }
    }

    fun capturePayment(foragePinEditText: ForagePINEditText, terminalId: String, paymentRef: String, onSuccess: (response: PosPaymentResponse?) -> Unit) {
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
                    val jsonAdapter: JsonAdapter<PosPaymentResponse> = PosPaymentResponseJsonAdapter(moshi)
                    val paymentResponse = jsonAdapter.fromJson(response.data)
                    _uiState.update { it.copy(capturePaymentResponse = paymentResponse, capturePaymentError = null) }
                    onSuccess(paymentResponse)

                    // we need to refetch the EBT Card here because
                    // capturing a payment does not include the updated
                    // balance we need to display the balance on the receipt
                    val paymentMethodRef = paymentResponse!!.paymentMethod
                    val updatedCard = api.getPaymentMethod(paymentMethodRef)
                    _uiState.update {
                        it.copy(tokenizedPaymentMethod = updatedCard)
                    }
                }
                is ForageApiResponse.Failure -> {
                    Log.e("POSViewModel", response.toString())
                    _uiState.update { it.copy(capturePaymentError = response.toString(), capturePaymentResponse = null) }
                }
            }
        }
    }

    fun refundPayment(foragePinEditText: ForagePINEditText, terminalId: String, amount: Float, paymentRef: String, reason: String, onSuccess: (response: Refund?) -> Unit) {
        viewModelScope.launch {
            val response = ForageTerminalSDK(terminalId).refundPayment(
                PosRefundPaymentParams(
                    foragePinEditText = foragePinEditText,
                    amount = amount,
                    paymentRef = paymentRef,
                    reason = reason
                )
            )
            var paymentMethod: PosPaymentMethod? = null

            try {
                val paymentResponse = api.getPayment(paymentRef)
                val paymentMethodResponse = api.getPaymentMethod(paymentResponse.paymentMethod)
                paymentMethod = paymentMethodResponse
            } catch (e: HttpException) {
                Log.e("POSViewModel", "Looking up payment method for refund failed. PaymentRef: $paymentRef")
            }

            when (response) {
                is ForageApiResponse.Success -> {
                    val moshi = Moshi.Builder().build()
                    val jsonAdapter: JsonAdapter<Refund> = RefundJsonAdapter(moshi)
                    val refundResponse = jsonAdapter.fromJson(response.data)
                    _uiState.update { it.copy(refundPaymentResponse = refundResponse, refundPaymentError = null, tokenizedPaymentMethod = paymentMethod) }
                    onSuccess(refundResponse)
                }
                is ForageApiResponse.Failure -> {
                    Log.e("POSViewModel", response.toString())
                    _uiState.update { it.copy(refundPaymentError = response.toString(), refundPaymentResponse = null) }
                }
            }
        }
    }

    fun voidPayment(paymentRef: String, onSuccess: (response: PosPaymentResponse) -> Unit) {
        val idempotencyKey = UUID.randomUUID().toString()

        viewModelScope.launch {
            try {
                var response = api.voidPayment(
                    idempotencyKey = idempotencyKey,
                    paymentRef = paymentRef
                )
                val payment = api.getPayment(paymentRef)
                val paymentMethod = api.getPaymentMethod(response.paymentMethod)
                if (response.receipt != null && payment.receipt != null) {
                    response.receipt!!.isVoided = true
                    response.receipt!!.balance.snap = (response.receipt!!.balance.snap.toDouble() + payment.receipt!!.snapAmount.toDouble()).toString()
                    response.receipt!!.balance.nonSnap = (response.receipt!!.balance.nonSnap.toDouble() + payment.receipt!!.ebtCashAmount.toDouble()).toString()
                }
                _uiState.update { it.copy(voidPaymentResponse = response, voidPaymentError = null, tokenizedPaymentMethod = paymentMethod) }
                onSuccess(response)
                Log.i("POSViewModel", "Void payment call succeeded: $response")
            } catch (e: HttpException) {
                Log.e("POSViewModel", "Void payment call failed: $e")
                _uiState.update { it.copy(voidPaymentError = e.toString(), voidPaymentResponse = null) }
            }
        }
    }

    fun voidRefund(paymentRef: String, refundRef: String, onSuccess: (response: Refund) -> Unit) {
        val idempotencyKey = UUID.randomUUID().toString()

        viewModelScope.launch {
            try {
                val payment = api.getPayment(paymentRef)
                val refund = api.getRefund(paymentRef, refundRef)
                val response = api.voidRefund(
                    idempotencyKey = idempotencyKey,
                    paymentRef = paymentRef,
                    refundRef = refundRef
                )
                val paymentMethod = api.getPaymentMethod(payment.paymentMethod)
                if (response.receipt != null && payment.receipt != null) {
                    response.receipt!!.isVoided = true
                    response.receipt!!.balance.snap = (response.receipt!!.balance.snap.toDouble() - refund.receipt!!.snapAmount.toDouble()).toString()
                    response.receipt!!.balance.nonSnap = (response.receipt!!.balance.nonSnap.toDouble() - refund.receipt!!.ebtCashAmount.toDouble()).toString()
                }
                _uiState.update { it.copy(voidRefundResponse = response, voidRefundError = null, tokenizedPaymentMethod = paymentMethod) }
                onSuccess(response)
                Log.i("POSViewModel", "Void refund call succeeded: $response")
            } catch (e: HttpException) {
                Log.e("POSViewModel", "Void refund call failed: $e")
                _uiState.update { it.copy(voidRefundError = e.toString(), voidRefundResponse = null) }
            }
        }
    }
}

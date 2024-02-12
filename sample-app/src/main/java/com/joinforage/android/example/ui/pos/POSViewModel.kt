package com.joinforage.android.example.ui.pos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joinforage.android.example.ui.pos.data.BalanceCheck
import com.joinforage.android.example.ui.pos.data.BalanceCheckJsonAdapter
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

class POSViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(POSUIState())
    val uiState: StateFlow<POSUIState> = _uiState.asStateFlow()
    private val api
        get() = PosApiService.from(uiState.value.posForageConfig)

    fun setSessionToken(sessionToken: String) {
        _uiState.update { it.copy(sessionToken = sessionToken) }
    }

    fun setMerchantId(merchantId: String, onSuccess: () -> Unit) {
        _uiState.update { it.copy(merchantId = merchantId) }
        onSuccess()
    }

    fun setLocalPayment(payment: PosPaymentRequest) {
        _uiState.update { it.copy(localPayment = payment) }
    }

    fun setLocalRefundState(refundState: RefundUIState, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val payment = api.getPayment(refundState.paymentRef)
                val paymentMethod = api.getPaymentMethod(payment.paymentMethod)
                _uiState.update { it.copy(localRefundState = refundState, tokenizedPaymentMethod = paymentMethod) }
                onComplete()
            } catch (e: HttpException) {
                _uiState.update { it.copy(localRefundState = refundState, tokenizedPaymentMethod = null) }
                onComplete()
            }
        }
    }

    fun fetchPayment(paymentRef: String) {
        viewModelScope.launch {
            try {
                val payment = api.getPayment(paymentRef)
                _uiState.update { it.copy(capturePaymentResponse = payment, capturePaymentError = null) }
            } catch (e: HttpException) {
                _uiState.update { it.copy(capturePaymentError = e.toString()) }
            }
        }
    }

    fun fetchRefund(paymentRef: String, refundRef: String) {
        viewModelScope.launch {
            try {
                val refund = api.getRefund(paymentRef, refundRef)
                _uiState.update { it.copy(refundPaymentResponse = refund, refundPaymentError = null) }
            } catch (e: HttpException) {
                _uiState.update { it.copy(refundPaymentError = e.toString()) }
            }
        }
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
                    sessionToken = it.sessionToken
                )
            }
        }
    }

    fun resetTokenizationError() {
        _uiState.update { it.copy(tokenizationError = null) }
    }

    fun resetPinActionErrors() {
        _uiState.update {
            it.copy(
                balanceCheckError = null,
                capturePaymentError = null,
                refundPaymentError = null
            )
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
            val response = ForageTerminalSDK(terminalId).init("").tokenizeCard(
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
            val forage = ForageTerminalSDK(terminalId).init("")
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
            val response = ForageTerminalSDK(terminalId).init("").checkBalance(
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

                    // we need to refetch the EBT Card here because
                    // `ForageTerminalSDK(terminalId).checkBalance`
                    // does not return the timestamp of the balance
                    // check and we need to display the timestamp
                    // on the receipt
                    val updatedCard = api.getPaymentMethod(paymentMethodRef)
                    _uiState.update {
                        it.copy(tokenizedPaymentMethod = updatedCard)
                    }
                    onSuccess(balance)
                }
                is ForageApiResponse.Failure -> {
                    Log.e("POSViewModel", response.toString())
                    _uiState.update { it.copy(balanceCheckError = response.toString(), balance = null) }
                }
            }
        }
    }

    fun capturePayment(foragePinEditText: ForagePINEditText, terminalId: String, paymentRef: String, onSuccess: () -> Unit, onFailure: (sequenceNumber: String?) -> Unit) {
        viewModelScope.launch {
            val response = ForageTerminalSDK(terminalId).init("").capturePayment(
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

                    // we need to refetch the EBT Card here because
                    // capturing a payment does not include the updated
                    // balance we need to display the balance on the receipt
                    val paymentMethodRef = paymentResponse!!.paymentMethod
                    val updatedCard = api.getPaymentMethod(paymentMethodRef)
                    _uiState.update {
                        it.copy(tokenizedPaymentMethod = updatedCard)
                    }
                    onSuccess()
                }
                is ForageApiResponse.Failure -> {
                    Log.e("POSViewModel", response.errors[0].message)
                    var payment: PosPaymentResponse? = null
                    try {
                        payment = api.getPayment(paymentRef)
                    } catch (e: HttpException) {
                        Log.e("POSViewModel", "Failed to re-fetch payment $paymentRef after failed capture")
                    }
                    _uiState.update { it.copy(capturePaymentError = response.errors[0].message, capturePaymentResponse = payment) }
                    onFailure(payment?.sequenceNumber)
                }
            }
        }
    }

    fun refundPayment(foragePinEditText: ForagePINEditText, terminalId: String, amount: Float, paymentRef: String, reason: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            val response = ForageTerminalSDK(terminalId).init("").refundPayment(
                PosRefundPaymentParams(
                    foragePinEditText = foragePinEditText,
                    amount = amount,
                    paymentRef = paymentRef,
                    reason = reason
                )
            )

            try {
                val payment = api.getPayment(paymentRef)
                val paymentMethod = api.getPaymentMethod(payment.paymentMethod)
                _uiState.update { it.copy(tokenizedPaymentMethod = paymentMethod, tokenizationError = null, capturePaymentResponse = payment, capturePaymentError = null) }
            } catch (e: HttpException) {
                Log.e("POSViewModel", "Looking up payment method for refund failed. PaymentRef: $paymentRef")
            }

            when (response) {
                is ForageApiResponse.Success -> {
                    val moshi = Moshi.Builder().build()
                    val jsonAdapter: JsonAdapter<Refund> = RefundJsonAdapter(moshi)
                    val refundResponse = jsonAdapter.fromJson(response.data)
                    _uiState.update { it.copy(refundPaymentResponse = refundResponse, refundPaymentError = null) }
                    onSuccess()
                }
                is ForageApiResponse.Failure -> {
                    Log.e("POSViewModel", response.toString())
                    var payment: PosPaymentResponse? = null
                    var refund: Refund? = null
                    try {
                        payment = api.getPayment(paymentRef)
                        val mostRecentRefundRef = payment.refunds.lastOrNull()
                        if (mostRecentRefundRef != null) {
                            refund = api.getRefund(paymentRef, mostRecentRefundRef)
                        }
                    } catch (e: HttpException) {
                        Log.e("POSViewModel", "Failed to re-fetch payment or refund after failed refund attempt. PaymentRef: $paymentRef")
                    }
                    _uiState.update { it.copy(refundPaymentError = response.errors[0].message, refundPaymentResponse = refund, capturePaymentResponse = payment) }
                    onFailure()
                }
            }
        }
    }

    fun voidPayment(paymentRef: String, onSuccess: (response: PosPaymentResponse) -> Unit) {
        val idempotencyKey = UUID.randomUUID().toString()

        viewModelScope.launch {
            try {
                val response = api.voidPayment(
                    idempotencyKey = idempotencyKey,
                    paymentRef = paymentRef
                )
                val payment = api.getPayment(paymentRef)
                val paymentMethod = api.getPaymentMethod(response.paymentMethod)
                if (response.receipt != null && payment.receipt != null) {
                    response.receipt!!.isVoided = true
                    response.receipt!!.balance?.snap = ((response.receipt!!.balance?.snap?.toDouble() ?: 0.0) + payment.receipt!!.snapAmount.toDouble()).toString()
                    response.receipt!!.balance?.nonSnap = ((response.receipt!!.balance?.nonSnap?.toDouble() ?: 0.0) + payment.receipt!!.ebtCashAmount.toDouble()).toString()
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
                if (payment.receipt != null) {
                    response.receipt!!.isVoided = true
                    response.receipt.balance?.snap = ((response.receipt.balance?.snap?.toDouble() ?: 0.0) - refund.receipt!!.snapAmount!!.toDouble()).toString()
                    response.receipt.balance?.nonSnap = ((response.receipt.balance?.nonSnap?.toDouble() ?: 0.0) - refund.receipt!!.ebtCashAmount!!.toDouble()).toString()
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

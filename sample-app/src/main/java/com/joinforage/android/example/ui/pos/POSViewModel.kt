package com.joinforage.android.example.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.POSUIState
import com.joinforage.android.example.ui.pos.network.PosApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

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

    fun setCardPAN(cardPAN: String) {
        _uiState.update { it.copy(cardPAN = cardPAN) }
    }

    private fun getMerchantInfo(merchantId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(merchantDetailsState = MerchantDetailsState.Loading) }
            val merchantDetailsState = try {
                val result = PosApi.retrofitService.getMerchantInfo(merchantId)
                onSuccess()
                MerchantDetailsState.Success(result)
            } catch (e: HttpException) {
                MerchantDetailsState.Error(e.toString())
            }
            _uiState.update { it.copy(merchantDetailsState = merchantDetailsState) }
        }
    }
}

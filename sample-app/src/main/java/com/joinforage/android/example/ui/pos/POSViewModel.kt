package com.joinforage.android.example.ui.pos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joinforage.android.example.ui.pos.data.POSUIState
import com.joinforage.android.example.ui.pos.network.PosApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.IOException

class POSViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(POSUIState())
    val uiState: StateFlow<POSUIState> = _uiState.asStateFlow()

    fun setMerchantId(merchantId: String) {
        _uiState.update { currentState ->
            currentState.copy(merchantId = merchantId)
        }
//        getMerchantInfo(merchantId = merchantId)
    }

    fun setCardPAN(cardPAN: String) {
        _uiState.update { currentState ->
            currentState.copy(cardPAN = cardPAN)
        }
    }

    private fun getMerchantInfo(merchantId: String) {
        viewModelScope.launch {
            try {
                val result = PosApi.retrofitService.getMerchantInfo(merchantId)
                _uiState.update { currentState ->
                    currentState.copy(merchantInfo = result)
                }
            } catch (e: IOException) {
                // just rethrow it for now while debugging
                throw e
            }
        }
    }
}

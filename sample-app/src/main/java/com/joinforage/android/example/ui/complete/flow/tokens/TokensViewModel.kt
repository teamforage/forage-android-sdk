package com.joinforage.android.example.ui.complete.flow.tokens

import androidx.lifecycle.MutableLiveData
import com.joinforage.android.example.ui.base.BaseViewModel
import com.joinforage.android.example.ui.complete.flow.tokens.model.TokensUIDefaultState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TokensViewModel @Inject constructor() : BaseViewModel() {
    val token = MutableLiveData(TokensUIDefaultState().bearer)
    val merchantAccount = MutableLiveData(TokensUIDefaultState().merchantAccount)

    fun getNextDestination() =
        FlowTokensFragmentDirections.actionNavigationCompleteFlowToFlowTokenizeFragment(
            bearer = token.value.orEmpty(),
            merchantAccount = merchantAccount.value.orEmpty()
        )
}

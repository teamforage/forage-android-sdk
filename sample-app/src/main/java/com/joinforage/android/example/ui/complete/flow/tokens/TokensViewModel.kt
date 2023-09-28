package com.joinforage.android.example.ui.complete.flow.tokens

import androidx.lifecycle.MutableLiveData
import com.joinforage.android.example.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TokensViewModel @Inject constructor() : BaseViewModel() {
    val token = MutableLiveData("dev_sessionToken1234")
    val merchantAccount = MutableLiveData("9876551")

    fun getNextDestination() =
        FlowTokensFragmentDirections.actionNavigationCompleteFlowToFlowTokenizeFragment(
            bearer = token.value.orEmpty(),
            merchantAccount = merchantAccount.value.orEmpty()
        )
}

package com.joinforage.android.example.ui.complete.flow.tokens

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.joinforage.android.example.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TokensViewModel @Inject constructor() : BaseViewModel() {
    val token = MutableLiveData("dev_sessionToken1234")
    val merchantAccount = MutableLiveData("9876551")

    fun getBundle(): Bundle = bundleOf(
        "bearer" to token.value.orEmpty(),
        "merchantAccount" to merchantAccount.value.orEmpty()
    )
}

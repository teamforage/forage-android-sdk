package com.joinforage.android.example.ui.complete.flow.tokens.model

import com.joinforage.android.example.BuildConfig

class TokensUIDefaultState(
    val isLoading: Boolean = false
) {
    val bearer = when (BuildConfig.FLAVOR) {
        "sandbox" -> "AbCaccesstokenXyz"
        "prod" -> "AbCaccesstokenXyz"
        else -> "AbCaccesstokenXyz"
    }

    val merchantAccount = when (BuildConfig.FLAVOR) {
        "sandbox" -> "9876545"
        "prod" -> "9876545"
        else -> "9876545"
    }
}

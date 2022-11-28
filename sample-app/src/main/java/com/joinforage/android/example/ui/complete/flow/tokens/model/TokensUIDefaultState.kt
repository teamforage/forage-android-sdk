package com.joinforage.android.example.ui.complete.flow.tokens.model

import com.joinforage.android.example.BuildConfig

class TokensUIDefaultState(
    val isLoading: Boolean = false
) {
    val bearer = when (BuildConfig.FLAVOR) {
        "dev" -> "AbCaccesstokenXyz"
        else -> "AbCaccesstokenXyz"
    }

    val merchantAccount = when (BuildConfig.FLAVOR) {
        "dev" -> "9876545"
        else -> "8000009"
    }
}

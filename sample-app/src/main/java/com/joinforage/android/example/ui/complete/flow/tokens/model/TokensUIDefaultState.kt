package com.joinforage.android.example.ui.complete.flow.tokens.model

import com.joinforage.android.example.BuildConfig

class TokensUIDefaultState(
    val isLoading: Boolean = false
) {
    val bearer = when (BuildConfig.FLAVOR) {
        "dev" -> "yzXZvJxXXF19L7bIRXDeISsMj7YLNK"
        else -> "IP0oRQBc16nFG5xsHP5ViyIdajyb7Z"
    }

    val merchantAccount = when (BuildConfig.FLAVOR) {
        "dev" -> "9876545"
        else -> "8000009"
    }
}

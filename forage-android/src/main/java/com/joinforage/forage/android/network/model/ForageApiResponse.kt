package com.joinforage.forage.android.network.model

sealed class ForageApiResponse<out T> {
    data class Success<out T>(val data: T) : ForageApiResponse<T>()

    data class Failure(val message: String) : ForageApiResponse<Nothing>()
}

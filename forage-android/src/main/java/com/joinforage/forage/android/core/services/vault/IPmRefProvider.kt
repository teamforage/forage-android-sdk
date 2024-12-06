package com.joinforage.forage.android.core.services.vault

internal interface IPmRefProvider {
    suspend fun getPaymentMethodRef(): String
}

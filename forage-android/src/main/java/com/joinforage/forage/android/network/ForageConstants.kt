package com.joinforage.forage.android.network

import com.joinforage.forage.android.BuildConfig
import okhttp3.Request

internal object ForageConstants {
    private const val BASE_URL = BuildConfig.BASE_URL

    fun provideHttpUrl() = Request.Builder().url(BASE_URL).build().url

    object Headers {
        const val X_KEY = "X-KEY"
        const val MERCHANT_ACCOUNT = "Merchant-Account"
        const val IDEMPOTENCY_KEY = "IDEMPOTENCY-KEY"
        const val AUTHORIZATION = "Authorization"
        const val BEARER = "Bearer"
        const val API_VERSION = "API-VERSION"
    }

    object RequestBody {
        const val CARD_NUMBER_TOKEN = "card_number_token"
    }

    object PathSegment {
        const val ISO_SERVER = "iso_server"
        const val ENCRYPTION_ALIAS = "encryption_alias"
        const val API = "api"
        const val PAYMENT_METHODS = "payment_methods"
        const val MESSAGE = "message"
        const val PAYMENTS = "payments"
    }

    object VGS {
        const val PIN_FIELD_NAME = "pin"
    }
}

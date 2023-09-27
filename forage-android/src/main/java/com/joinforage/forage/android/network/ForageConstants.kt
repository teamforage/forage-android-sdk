package com.joinforage.forage.android.network

import com.joinforage.forage.android.core.StopgapGlobalState
import com.joinforage.forage.android.network.model.ForageError
import okhttp3.Request

internal object ForageConstants {
    private val BASE_URL = StopgapGlobalState.envConfig.baseUrl

    fun provideHttpUrl() = Request.Builder().url(BASE_URL).build().url

    object Headers {
        const val X_KEY = "X-KEY"
        const val MERCHANT_ACCOUNT = "Merchant-Account"
        const val IDEMPOTENCY_KEY = "IDEMPOTENCY-KEY"
        const val TRACE_ID = "x-datadog-trace-id"
        const val AUTHORIZATION = "Authorization"
        const val BEARER = "Bearer"
        const val API_VERSION = "API-VERSION"
        const val BT_PROXY_KEY = "BT-PROXY-KEY"
        const val CONTENT_TYPE = "Content-Type"
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

    object ErrorResponseObjects {
        val INCOMPLETE_PIN_ERROR = listOf(
            ForageError(400, "user_error", "Invalid EBT Card PIN entered. Please enter your 4-digit PIN.")
        )
    }
}

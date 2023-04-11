package com.joinforage.forage.android.fixtures

import me.jorgecastillo.hiroaki.Method
import me.jorgecastillo.hiroaki.models.PotentialRequestChain
import me.jorgecastillo.hiroaki.models.error
import me.jorgecastillo.hiroaki.models.fileBody
import me.jorgecastillo.hiroaki.models.json
import me.jorgecastillo.hiroaki.models.success
import me.jorgecastillo.hiroaki.whenever
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.givenCardNumberWithUserId(cardNumber: String, userId: String) = whenever(
    method = Method.POST,
    sentToPath = "api/payment_methods/",
    jsonBody = json {
        "type" / "ebt"
        "reusable" / true
        "card" / json {
            "number" / cardNumber
        }
        "user_id" / userId
    }
)

fun MockWebServer.givenCardNumberWithoutUserId(cardNumber: String) = whenever(
    method = Method.POST,
    sentToPath = "api/payment_methods/",
    jsonBody = json {
        "type" / "ebt"
        "reusable" / true
        "card" / json {
            "number" / cardNumber
        }
    }
)

fun PotentialRequestChain.returnsPaymentMethodSuccessfully(withUserId: Boolean) = thenRespond(
    success(
        jsonBody = if (withUserId) {
            fileBody(
                "fixtures/payment/methods/successful_create_payment_method_with_userid.json"
            )
        } else {
            fileBody(
                "fixtures/payment/methods/successful_create_payment_method_without_userid.json"
            )
        }
    )
)

fun PotentialRequestChain.returnsPaymentMethodFailed() = thenRespond(
    error(
        400,
        jsonBody = fileBody(
            "fixtures/payment/methods/failed_create_payment_method.json"
        )
    )
)

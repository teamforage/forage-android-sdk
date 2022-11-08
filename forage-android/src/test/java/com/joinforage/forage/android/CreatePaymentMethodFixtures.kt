package com.joinforage.forage.android

import me.jorgecastillo.hiroaki.Method
import me.jorgecastillo.hiroaki.models.PotentialRequestChain
import me.jorgecastillo.hiroaki.models.fileBody
import me.jorgecastillo.hiroaki.models.json
import me.jorgecastillo.hiroaki.models.success
import me.jorgecastillo.hiroaki.whenever
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.givenCardToken(cardNumber: String) = whenever(
    method = Method.POST,
    sentToPath = "payment_methods/",
    jsonBody = json {
        "type" / "ebt"
        "reusable" / true
        "card" / json {
            "number" / cardNumber
        }
    }
)

fun PotentialRequestChain.returnsPaymentMethodSuccessfully() = thenRespond(
    success(
        jsonBody = fileBody(
            "fixtures/payment/methods/successful_create_payment_method.json"
        )
    )
)

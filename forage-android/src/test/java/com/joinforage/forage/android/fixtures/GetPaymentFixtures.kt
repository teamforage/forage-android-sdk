package com.joinforage.forage.android.fixtures

import me.jorgecastillo.hiroaki.Method
import me.jorgecastillo.hiroaki.models.PotentialRequestChain
import me.jorgecastillo.hiroaki.models.fileBody
import me.jorgecastillo.hiroaki.models.success
import me.jorgecastillo.hiroaki.models.error
import me.jorgecastillo.hiroaki.whenever
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.givenPaymentRef() = whenever(
    method = Method.GET,
    sentToPath = "api/payments/6ae6a45ff1"
)

fun PotentialRequestChain.returnsPayment() = thenRespond(
    success(
        jsonBody = fileBody(
            "fixtures/payments/capture/successful_payment_response.json"
        )
    )
)

fun PotentialRequestChain.returnsFailedPayment() = thenRespond(
    error(
        404,
        jsonBody = fileBody(
            "fixtures/payments/capture/failed_get_payment.json"
        )
    )
)

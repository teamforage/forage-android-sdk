package com.joinforage.forage.android.fixtures

import me.jorgecastillo.hiroaki.Method
import me.jorgecastillo.hiroaki.models.PotentialRequestChain
import me.jorgecastillo.hiroaki.models.error
import me.jorgecastillo.hiroaki.models.fileBody
import me.jorgecastillo.hiroaki.models.success
import me.jorgecastillo.hiroaki.whenever
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.givenPaymentMethodRef() = whenever(
    method = Method.GET,
    sentToPath = "api/payment_methods/1f148fe399"
)

fun PotentialRequestChain.returnsPaymentMethod() = thenRespond(
    success(
        jsonBody = fileBody(
            "fixtures/payment/methods/successful_create_payment_method.json"
        )
    )
)

fun PotentialRequestChain.returnsPaymentMethodWithBalance() = thenRespond(
    success(
        jsonBody = fileBody(
            "fixtures/payment/methods/get_payment_method_with_balance.json"
        )
    )
)

fun PotentialRequestChain.returnsFailedPaymentMethod() = thenRespond(
    error(
        404,
        jsonBody = fileBody(
            "fixtures/payment/methods/failed_get_payment_method.json"
        )
    )
)

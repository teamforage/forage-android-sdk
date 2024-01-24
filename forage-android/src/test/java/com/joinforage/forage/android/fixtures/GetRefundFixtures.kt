package com.joinforage.forage.android.fixtures

import me.jorgecastillo.hiroaki.Method
import me.jorgecastillo.hiroaki.models.PotentialRequestChain
import me.jorgecastillo.hiroaki.models.error
import me.jorgecastillo.hiroaki.models.fileBody
import me.jorgecastillo.hiroaki.models.success
import me.jorgecastillo.hiroaki.whenever
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.givenPaymentAndRefundRef() = whenever(
    method = Method.GET,
    sentToPath = "api/payments/6ae6a45ff1/refunds/refund123"
)

fun PotentialRequestChain.returnsRefund() = thenRespond(
    success(
        jsonBody = fileBody(
            "fixtures/refunds/successful_get_refund.json"
        )
    )
)

fun PotentialRequestChain.returnsFailedRefund() = thenRespond(
    error(
        404,
        jsonBody = fileBody(
            "fixtures/refunds/failed_get_refund.json"
        )
    )
)

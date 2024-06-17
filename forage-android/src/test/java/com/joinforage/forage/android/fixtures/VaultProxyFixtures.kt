package com.joinforage.forage.android.fixtures

import me.jorgecastillo.hiroaki.Method
import me.jorgecastillo.hiroaki.models.PotentialRequestChain
import me.jorgecastillo.hiroaki.models.error
import me.jorgecastillo.hiroaki.models.fileBody
import me.jorgecastillo.hiroaki.models.inlineBody
import me.jorgecastillo.hiroaki.whenever
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.givenRosettaPaymentCaptureRequest(paymentRef: String) = whenever(
    method = Method.POST,
    sentToPath = "proxy/api/payments/$paymentRef/capture"
)

fun PotentialRequestChain.returnsRosettaError() = thenRespond(
    error(
        401,
        jsonBody = fileBody(
            "fixtures/vault/rosetta_vault_error_response.json"
        )
    )
)

fun PotentialRequestChain.returnsMalformedError() = thenRespond(
    error(
        504,
        jsonBody = inlineBody("{}")
    )
)

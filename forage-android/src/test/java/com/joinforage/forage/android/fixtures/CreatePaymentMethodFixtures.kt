package com.joinforage.forage.android.fixtures

import me.jorgecastillo.hiroaki.Method
import me.jorgecastillo.hiroaki.models.PotentialRequestChain
import me.jorgecastillo.hiroaki.models.error
import me.jorgecastillo.hiroaki.models.fileBody
import me.jorgecastillo.hiroaki.models.json
import me.jorgecastillo.hiroaki.models.success
import me.jorgecastillo.hiroaki.whenever
import okhttp3.mockwebserver.MockWebServer

internal fun MockWebServer.givenPaymentMethod(cardNumber: String, customerId: String, reusable: Boolean = true) = whenever(
    method = Method.POST,
    sentToPath = "api/payment_methods/",
    jsonBody = json {
        "type" / "ebt"
        "reusable" / reusable
        "card" / json {
            "number" / cardNumber
        }
        "customer_id" / customerId
    }
)

internal fun MockWebServer.givenPaymentMethod(cardNumber: String, reusable: Boolean = true) = whenever(
    method = Method.POST,
    sentToPath = "api/payment_methods/",
    jsonBody = json {
        "type" / "ebt"
        "reusable" / reusable
        "card" / json {
            "number" / cardNumber
        }
    }
)

internal fun PotentialRequestChain.returnsPaymentMethodSuccessfully() = thenRespond(
    success(
        jsonBody = fileBody(
            "fixtures/payment/methods/successful_create_payment_method.json"
        )
    )
)

internal fun PotentialRequestChain.returnsMissingCustomerIdPaymentMethodSuccessfully() = thenRespond(
    success(
        jsonBody = fileBody(
            "fixtures/payment/methods/successful_create_payment_method_without_customer_id.json"
        )
    )
)

internal fun PotentialRequestChain.returnsNonReusablePaymentMethodSuccessfully() = thenRespond(
    success(
        jsonBody = fileBody(
            "fixtures/payment/methods/successful_create_nonreusable_payment_method.json"
        )
    )
)

internal fun PotentialRequestChain.returnsPaymentMethodFailed() = thenRespond(
    error(
        400,
        jsonBody = fileBody(
            "fixtures/payment/methods/failed_create_payment_method.json"
        )
    )
)

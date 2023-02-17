package com.joinforage.forage.android.fixtures

import me.jorgecastillo.hiroaki.Method
import me.jorgecastillo.hiroaki.models.PotentialRequestChain
import me.jorgecastillo.hiroaki.models.error
import me.jorgecastillo.hiroaki.models.fileBody
import me.jorgecastillo.hiroaki.models.success
import me.jorgecastillo.hiroaki.whenever
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.givenContentId(contentId: String) = whenever(
    method = Method.GET,
    sentToPath = "api/message/$contentId"
)

fun PotentialRequestChain.returnsMessageCompletedSuccessfully() = thenRespond(
    success(
        jsonBody = fileBody(
            "fixtures/message/successful_completed_message.json"
        )
    )
)

fun PotentialRequestChain.returnsUnauthorized() = thenRespond(
    error(
        401,
        jsonBody = fileBody(
            "fixtures/message/unauthorized_get_message.json"
        )
    )
)

fun PotentialRequestChain.returnsFailed() = thenRespond(
    success(
        jsonBody = fileBody(
            "fixtures/message/failed_get_message.json"
        )
    )
)

fun PotentialRequestChain.returnsSendToProxy() = thenRespond(
    success(
        jsonBody = fileBody(
            "fixtures/message/send_to_proxy_message.json"
        )
    )
)

fun PotentialRequestChain.returnsExpiredCard() = thenRespond(
    success(
        jsonBody = fileBody(
            "fixtures/message/expired_card_message_response.json"
        )
    )
)

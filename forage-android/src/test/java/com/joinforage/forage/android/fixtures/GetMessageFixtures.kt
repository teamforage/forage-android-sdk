package com.joinforage.forage.android.fixtures

import me.jorgecastillo.hiroaki.models.PotentialRequestChain
import me.jorgecastillo.hiroaki.models.error
import me.jorgecastillo.hiroaki.models.fileBody

fun PotentialRequestChain.returnsUnauthorized() = thenRespond(
    error(
        401,
        jsonBody = fileBody(
            "fixtures/message/unauthorized_get_message.json"
        )
    )
)

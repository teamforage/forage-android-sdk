package com.joinforage.forage.android.fixtures

import me.jorgecastillo.hiroaki.Method
import me.jorgecastillo.hiroaki.models.PotentialRequestChain
import me.jorgecastillo.hiroaki.models.error
import me.jorgecastillo.hiroaki.models.fileBody
import me.jorgecastillo.hiroaki.models.success
import me.jorgecastillo.hiroaki.whenever
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.givenEncryptionKey() = whenever(
    method = Method.GET,
    sentToPath = "iso_server/encryption_alias"
)

fun PotentialRequestChain.returnsEncryptionKeySuccessfully() = thenRespond(
    success(
        jsonBody = fileBody(
            "fixtures/encryption/key/successful_get_encryption_key.json"
        )
    )
)

fun PotentialRequestChain.returnsUnauthorizedEncryptionKey() = thenRespond(
    error(
        jsonBody = fileBody(
            "fixtures/encryption/key/unauthorized_get_encryption_key.json"
        )
    )
)

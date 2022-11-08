package com.joinforage.forage.android.network.mapper

import com.joinforage.forage.android.network.model.Response
import com.verygoodsecurity.vgscollect.core.model.network.VGSResponse

fun VGSResponse.toResponse(): Response = when (this) {
    is VGSResponse.ErrorResponse -> Response.ErrorResponse(
        localizeMessage = localizeMessage,
        errorCode = errorCode,
        rawResponse = body
    )
    is VGSResponse.SuccessResponse -> Response.SuccessResponse(
        successCode = successCode,
        rawResponse = body
    )
}

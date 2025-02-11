package com.joinforage.forage.android.core.services.forageapi.network.error

import com.joinforage.forage.android.core.services.forageapi.network.error.payload.DeferredRefundErrorResponsePayload
import com.joinforage.forage.android.core.services.forageapi.network.error.payload.ErrorListResponsePayload
import com.joinforage.forage.android.core.services.forageapi.network.error.payload.ErrorPayload
import com.joinforage.forage.android.core.services.forageapi.network.error.payload.RosettaBadRequestResponsePayload
import com.joinforage.forage.android.core.services.forageapi.network.error.payload.RosettaErrorResponsePayload
import com.joinforage.forage.android.core.services.forageapi.network.error.payload.SingleErrorResponsePayload
import org.json.JSONObject

internal class PosErrorResponseParser : ForageErrorResponseParser() {
    override fun parseJson(jsonErrorResponse: JSONObject): ErrorPayload {
        val deferred = DeferredRefundErrorResponsePayload(jsonErrorResponse)
        if (deferred.isMatch()) return deferred
        val single = SingleErrorResponsePayload(jsonErrorResponse)
        if (single.isMatch()) return single
        val list = ErrorListResponsePayload(jsonErrorResponse)
        if (list.isMatch()) return list
        val bad = RosettaBadRequestResponsePayload(jsonErrorResponse)
        if (bad.isMatch()) return bad
        val error = RosettaErrorResponsePayload(jsonErrorResponse)
        if (error.isMatch()) return error
        throw ErrorPayload.UnknownForageFailureResponse(jsonErrorResponse.toString())
    }
}

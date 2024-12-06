package com.joinforage.forage.android.core.services.vault.requests

import org.json.JSONObject

interface IBaseBodyBuilder {
    fun build(body: JSONObject = JSONObject()): JSONObject
}
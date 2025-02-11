package com.joinforage.forage.android.core.services.forageapi.engine

import com.joinforage.forage.android.core.services.forageapi.network.error.EcomErrorResponseParser

internal class EcomOkHttpEngine : BaseOkHttpEngine(EcomErrorResponseParser())

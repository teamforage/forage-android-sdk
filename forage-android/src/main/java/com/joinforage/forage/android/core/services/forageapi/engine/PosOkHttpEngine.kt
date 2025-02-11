package com.joinforage.forage.android.core.services.forageapi.engine

import com.joinforage.forage.android.core.services.forageapi.network.error.PosErrorResponseParser

internal class PosOkHttpEngine : BaseOkHttpEngine(PosErrorResponseParser())

package com.joinforage.forage.android.pos.services.forageapi.engine

import com.joinforage.forage.android.core.services.forageapi.engine.BaseOkHttpEngine
import com.joinforage.forage.android.pos.services.network.error.PosErrorResponseParser

internal class PosOkHttpEngine : BaseOkHttpEngine(PosErrorResponseParser())

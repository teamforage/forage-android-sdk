package com.joinforage.forage.android.ecom.services.forageapi.engine

import com.joinforage.forage.android.core.services.forageapi.engine.BaseOkHttpEngine
import com.joinforage.forage.android.ecom.services.network.error.EcomErrorResponseParser

internal class EcomOkHttpEngine : BaseOkHttpEngine(EcomErrorResponseParser())

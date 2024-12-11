package com.joinforage.forage.android.core.services.vault.metrics

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse

internal interface IMetricsRecorder {
    fun recordMetrics(response: ForageApiResponse<String>)
    fun startCustomerPerceptionMetric()
    fun startVaultProxyMetric()
}

package com.joinforage.forage.android.core.services.vault.metrics

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.telemetry.CustomerPerceivedMetricLogger
import com.joinforage.forage.android.core.services.telemetry.LogLogger
import com.joinforage.forage.android.core.services.telemetry.VaultProxyMetricLogger
import javax.inject.Inject

internal class VaultMetricsRecorder @Inject constructor(private val logger: LogLogger) : IMetricsRecorder {
    private var proxyMetrics: VaultProxyMetricLogger? = null
    private var customerMetrics: CustomerPerceivedMetricLogger? = null

    override fun startVaultProxyMetric() {
        proxyMetrics = VaultProxyMetricLogger(logger)
    }

    override fun startCustomerPerceptionMetric() {
        customerMetrics = CustomerPerceivedMetricLogger(logger)
    }

    override fun recordMetrics(response: ForageApiResponse<String>) {
        proxyMetrics!!.logResult(response)
        customerMetrics!!.logResult(response)
    }
}

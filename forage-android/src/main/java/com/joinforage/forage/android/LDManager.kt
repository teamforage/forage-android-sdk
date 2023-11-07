package com.joinforage.forage.android

import android.app.Application
import com.joinforage.forage.android.core.telemetry.Log
import com.launchdarkly.sdk.ContextKind
import com.launchdarkly.sdk.LDContext
import com.launchdarkly.sdk.LDValue
import com.launchdarkly.sdk.android.LDClient
import com.launchdarkly.sdk.android.LDConfig
import com.launchdarkly.sdk.android.integrations.TestData

internal enum class VaultType(val value: String) {
    VGS_VAULT_TYPE("vgs"),
    BT_VAULT_TYPE("basis_theory");

    override fun toString(): String {
        return value
    }
}

internal object LDFlags {
    const val VAULT_PRIMARY_TRAFFIC_PERCENTAGE_FLAG = "vault-primary-traffic-percentage"
    const val ISO_POLLING_WAIT_INTERVALS = "iso-polling-wait-intervals"
}

internal object LDContexts {
    const val ANDROID_CONTEXT = "android-sdk-service"
}

internal object LDContextKind {
    const val SERVICE = "service"
}

internal val ALWAYS_BT = 100.0
internal val ALWAYS_VGS = 0.0

internal fun computeVaultType(trafficPrimaryPercentFlag: Double): VaultType {
    val randomNum = Math.random() * 100
    return if (randomNum < trafficPrimaryPercentFlag) VaultType.BT_VAULT_TYPE else VaultType.VGS_VAULT_TYPE
}

internal object LDManager {
    private var internalLogger: Log = Log.getSilentInstance()
    private var client: LDClient? = null

    // default to 100% VGS usage in case LD flag retrieval fails
    private var primaryTrafficPercent = ALWAYS_VGS

    // we only ever want to fetch primary traffic percent flag once
    // so that all parts of the code are guaranteed to use the same
    // value
    private var hasFetchedPrimaryTrafficPercentFlag = false

    fun createLdConfig(ldMobileKey: String): LDConfig {
        return LDConfig.Builder()
            .mobileKey(ldMobileKey)
            .build()
    }
    fun TEST_createLdConfig(ldMobileKey: String, dataSource: TestData): LDConfig {
        return LDConfig.Builder()
            .mobileKey(ldMobileKey)
            .dataSource(dataSource)
            .build()
    }

    internal fun initialize(app: Application, logger: Log, ldConfig: LDConfig) {
        val contextKind = ContextKind.of(LDContextKind.SERVICE)
        val context = LDContext.create(contextKind, LDContexts.ANDROID_CONTEXT)
        client = LDClient.init(app, ldConfig, context, 0)

        internalLogger = logger
    }

    internal fun TEST_clearPrimaryTrafficPercentCache() {
        hasFetchedPrimaryTrafficPercentFlag = false
    }

    // We need to ensure that all subsequent calls to getVaultProvider
    // return the same vault provider for all parts of the codebase.
    // ForagePINEditText is the entrypoint and is responsible for
    // initializing LDManager and calling getVaultProvider initially
    // before ForageSDK can reference these values
    internal fun getVaultProvider(): VaultType {
        primaryTrafficPercent = if (hasFetchedPrimaryTrafficPercentFlag) {
            // return the cached value if we already tried to fetch the flag
            primaryTrafficPercent
        } else {
            // indicate that we've attempted to fetch the flag so we don't do it again
            hasFetchedPrimaryTrafficPercentFlag = true

            // fetch the flag
            val vaultPercent =
                client?.doubleVariation(LDFlags.VAULT_PRIMARY_TRAFFIC_PERCENTAGE_FLAG, ALWAYS_VGS) ?: ALWAYS_VGS
            internalLogger.i("[LaunchDarkly] Vault percent of $vaultPercent return from LD")

            // return the flag value
            vaultPercent
        }

        // convert the flag percent into an answer to which vault provider to use
        val vaultType = computeVaultType(primaryTrafficPercent)
        internalLogger.i("[LaunchDarkly] Vault type set to $vaultType")

        // return vault provider derived from the novel or cached flag value
        return vaultType
    }

    internal fun getPollingIntervals(): LongArray {
        val defaultVal = LDValue.buildObject().put(
            "intervals",
            LDValue.Convert.Long.arrayFrom(List(10) { 1000L })
        ).build()

        val jsonIntervals = client?.jsonValueVariation(LDFlags.ISO_POLLING_WAIT_INTERVALS, defaultVal) ?: defaultVal

        val intervals = jsonIntervals.get("intervals")

        // Converting the LDArray into a LongArray
        val pollingList = LongArray(intervals.size()) { intervals.get(it).longValue() }

        internalLogger.i("[LaunchDarkly] polling intervals $pollingList")
        return pollingList
    }
}

package com.joinforage.forage.android

import android.app.Application
import com.joinforage.forage.android.core.telemetry.Log
import com.launchdarkly.sdk.ContextKind
import com.launchdarkly.sdk.LDContext
import com.launchdarkly.sdk.LDValue
import com.launchdarkly.sdk.android.LDClient
import com.launchdarkly.sdk.android.LDConfig

internal enum class VaultType(val value: String) {
    VGS_VAULT_TYPE("vgs"),
    BT_VAULT_TYPE("basis_theory"),
    FORAGE_VAULT_TYPE("forage");

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

internal val ALWAYS_BT_PERCENT = 100.0
internal val ALWAYS_VGS_PERCENT = 0.0

internal fun computeVaultType(trafficPrimaryPercentFlag: Double): VaultType {
    val randomNum = Math.random() * 100
    return if (randomNum < trafficPrimaryPercentFlag) VaultType.BT_VAULT_TYPE else VaultType.VGS_VAULT_TYPE
}

internal object LDManager {
    // as much as I would LOVE to not have this be a shared state on a
    // singleton object, Launch Darkly literally tells us to make it
    // a singleton :/
    //
    // https://docs.launchdarkly.com/sdk/client-side/android#:~:text=LDClient%20must%20be%20a%20singleton
    private var client: LDClient? = null

    internal fun initialize(app: Application, ldConfig: LDConfig) {
        val contextKind = ContextKind.of(LDContextKind.SERVICE)
        val context = LDContext.create(contextKind, LDContexts.ANDROID_CONTEXT)
        client = LDClient.init(app, ldConfig, context, 1)
    }

    internal fun getVaultProvider(logger: Log = Log.getSilentInstance()): VaultType {
        val vaultPercent = client?.doubleVariation(
            LDFlags.VAULT_PRIMARY_TRAFFIC_PERCENTAGE_FLAG,
            ALWAYS_VGS_PERCENT
        ) ?: ALWAYS_VGS_PERCENT
        logger.i("[LaunchDarkly] Vault percent of $vaultPercent return from LD")

        // convert the flag percent into an answer to which vault provider to use
        val vaultType = computeVaultType(vaultPercent)
        logger.i("[LaunchDarkly] Vault type set to $vaultType")

        // return vault provider
        return vaultType
    }

    internal fun getPollingIntervals(logger: Log = Log.getSilentInstance()): LongArray {
        val defaultVal = LDValue.buildObject().put(
            "intervals",
            LDValue.Convert.Long.arrayFrom(List(10) { 1000L })
        ).build()

        val jsonIntervals = client?.jsonValueVariation(
            LDFlags.ISO_POLLING_WAIT_INTERVALS,
            defaultVal
        ) ?: defaultVal

        val intervals = jsonIntervals.get("intervals")

        // Converting the LDArray into a LongArray
        val pollingList = LongArray(intervals.size()) { intervals.get(it).longValue() }

        logger.i("[LaunchDarkly] polling intervals $pollingList")
        return pollingList
    }
}

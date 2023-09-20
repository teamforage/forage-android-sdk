package com.joinforage.forage.android

import android.app.Application
import com.joinforage.forage.android.core.StopgapGlobalState
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

internal object LDManager {
    private val LD_MOBILE_KEY = StopgapGlobalState.envConfig.ldMobileKey
    private var internalVaultType: VaultType? = null

    private var internalLogger: Log = Log.getSilentInstance()
    private var client: LDClient? = null

    internal var vaultType: VaultType?
        get() = internalVaultType

        // The setter is only exposed for testing purposes. Otherwise, it is entirely internal and
        // shouldn't be used directly.
        set(value) {
            if (value == null) {
                internalLogger.w("[LaunchDarkly] vaultType is being reset to null. This should only happen while unit testing!")
                internalVaultType = null
            } else if (internalVaultType == null) {
                internalVaultType = value
            } else {
                throw Error("vaultType can only be set once!")
            }
        }

    internal fun initialize(app: Application, logger: Log, dataSource: TestData? = null) {
        val ldConfig = if (dataSource != null) {
            LDConfig.Builder()
                .mobileKey(LD_MOBILE_KEY)
                .dataSource(dataSource)
                .build()
        } else {
            LDConfig.Builder()
                .mobileKey(LD_MOBILE_KEY)
                .build()
        }
        val contextKind = ContextKind.of(LDContextKind.SERVICE)
        val context = LDContext.create(contextKind, LDContexts.ANDROID_CONTEXT)
        client = LDClient.init(app, ldConfig, context, 0)

        internalLogger = logger
    }

    // vaultType is instantiated lazily and is a singleton. Once we set the vault type once, we don't
    // want to overwrite it! We must take in the application as a parameter, which means that a
    // ForagePINEditText must be rendered before any of the ForageSDKApi functions are called.
    internal fun getVaultProvider(): VaultType {
        if (vaultType != null) {
            return vaultType as VaultType
        }
        // default to 100% VGS usage in case LD flag retrieval fails
        val defaultVal = 0.0
        val vaultPercent =
            client?.doubleVariation(LDFlags.VAULT_PRIMARY_TRAFFIC_PERCENTAGE_FLAG, defaultVal) ?: defaultVal
        internalLogger.i("[LaunchDarkly] Vault percent of $vaultPercent return from LD")
        val randomNum = Math.random() * 100

        vaultType = if (randomNum < vaultPercent) {
            VaultType.BT_VAULT_TYPE
        } else {
            VaultType.VGS_VAULT_TYPE
        }
        internalLogger.i("[LaunchDarkly] Vault type set to $vaultType")
        return vaultType as VaultType
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

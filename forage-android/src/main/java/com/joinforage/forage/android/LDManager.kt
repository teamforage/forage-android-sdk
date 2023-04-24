package com.joinforage.forage.android
import android.app.Application
import com.joinforage.forage.android.core.Logger
import com.launchdarkly.sdk.LDContext
import com.launchdarkly.sdk.android.LDClient
import com.launchdarkly.sdk.android.LDConfig
import com.launchdarkly.sdk.android.integrations.TestData

internal object VaultConstants {
    const val VGS_VAULT_TYPE = "vgs_vault_type"
    const val BT_VAULT_TYPE = "bt_vault_type"
}

internal object LDFlags {
    const val VAULT_PRIMARY_TRAFFIC_PERCENTAGE_FLAG = "vault-primary-traffic-percentage"
}

internal object LDContexts {
    const val USER = "anonymous-user"
}

object LDManager {
    private const val LD_MOBILE_KEY = BuildConfig.LD_MOBILE_KEY
    private var internalVaultType: String? = null
    private var logger = Logger.getInstance(BuildConfig.DEBUG)

    internal var vaultType: String?
        get() = internalVaultType
        set(value) {
            if (value == null) {
                logger.warning("vaultType is being reset to null. This should only happen while unit testing!")
                internalVaultType = null
            } else if (internalVaultType == null) {
                internalVaultType = value
            } else {
                logger.warning("vaultType can only be set once!")
            }
        }

    // vaultType is instantiated lazily and is a singleton. Once we set the vault type once, we don't
    // want to overwrite it! We must take in the application as a parameter, which means that a
    // ForagePINEditText must be rendered before any of the ForageSDKApi functions are called.
    internal fun getVaultProvider(app: Application, dataSource: TestData? = null): String {
        if (vaultType != null) {
            return vaultType as String
        }
        // Datasource is required for testing purposes!
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
        val context = LDContext.create(LDContexts.USER)
        val client = LDClient.init(app, ldConfig, context, 0)
        // default to 100% BT usage in case LD flag retrieval fails
        val vaultPercent = client.doubleVariation(LDFlags.VAULT_PRIMARY_TRAFFIC_PERCENTAGE_FLAG, 100.0)
        val randomNum = Math.random() * 100
        vaultType = if (randomNum < vaultPercent) {
            VaultConstants.BT_VAULT_TYPE
        } else {
            VaultConstants.VGS_VAULT_TYPE
        }
        return vaultType as String
    }
}

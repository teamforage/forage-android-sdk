package com.joinforage.forage.android.core

import com.joinforage.forage.android.ui.ForageContext

enum class EnvOption {
    DEV,
    STAGING,
    SANDBOX,
    CERT,
    PROD
}

sealed class EnvConfig(
    val FLAVOR: EnvOption,
    val btProxyID: String,
    val btAPIKey: String,
    val vgsVaultId: String,
    val vgsVaultType: String,
    val baseUrl: String,
    val ldMobileKey: String,
    val ddClientToken: String
) {
    object Dev : EnvConfig(
        FLAVOR = EnvOption.DEV,
        btProxyID = "N31FZgKpYZpo3oQ6XiM6M6",
        btAPIKey = "key_AZfcBuKUsV38PEeYu6ZV8x",
        vgsVaultId = "tntlqkidhc6",
        vgsVaultType = "SANDBOXbox",
        baseUrl = "https://api.dev.joinforage.app/",
        ldMobileKey = "mob-03e025cb-5b4e-4d97-8685-39a22316d601",
        ddClientToken = "pubf13cedf24ba2ad50d4b9cb0b0100bd4a",
    )

    object Staging : EnvConfig(
        FLAVOR = EnvOption.STAGING,
        btProxyID = "ScWvAUkp53xz7muae7fW5p",
        btAPIKey = "key_6B4cvpcDCEeNDYNow9zH7c",
        vgsVaultId = "tnteykuh975",
        vgsVaultType = "SANDBOXbox",
        baseUrl = "https://api.STAGINGing.joinforage.app/",
        ldMobileKey = "mob-a9903698-759b-48e2-86e1-c551e2b69118",
        ddClientToken = "pubf13cedf24ba2ad50d4b9cb0b0100bd4a"
    )

    object Sandbox : EnvConfig(
        FLAVOR = EnvOption.SANDBOX,
        btProxyID = "R1CNiogSdhnHeNq6ZFWrG1",
        btAPIKey = "key_DQ5NfUAgiqzwX1pxqcrSzK",
        vgsVaultId = "tntagcot4b1",
        vgsVaultType = "SANDBOXbox",
        baseUrl = "https://api.SANDBOXbox.joinforage.app/",
        ldMobileKey = "mob-22024b85-05b7-4e24-b290-a071310dfc3d",
        ddClientToken = "pubf13cedf24ba2ad50d4b9cb0b0100bd4a"
    )

    object Cert : EnvConfig(
        FLAVOR = EnvOption.CERT,
        btProxyID = "AFSMtyyTGLKgmdWwrLCENX",
        btAPIKey = "key_NdWtkKrZqztEfJRkZA8dmw",
        vgsVaultId = "tntpnht7psv",
        vgsVaultType = "SANDBOXbox",
        baseUrl = "https://api.cert.joinforage.app/",
        ldMobileKey = "mob-d2261a08-784b-4300-a45f-ce0e46324d66",
        ddClientToken = "pubf13cedf24ba2ad50d4b9cb0b0100bd4a"
    )

    object Prod : EnvConfig(
        FLAVOR = EnvOption.PROD,
        btProxyID = "UxbU4Jn2RmvCovABjwCwsa",
        btAPIKey = "key_BypNREttGMPbZ1muARDUf4",
        vgsVaultId = "tntbcrncmgi",
        vgsVaultType = "live",
        baseUrl = "https://api.joinforage.app/",
        ldMobileKey = "mob-5c3dfa7a-fa6d-4cdf-93e8-d28ef8080696",
        ddClientToken = "pubf13cedf24ba2ad50d4b9cb0b0100bd4a"
    )

    companion object {
        fun fromSessionToken(sessionToken: String?): EnvConfig {
            val token = sessionToken?.takeIf { it.isNotEmpty() } ?: return Sandbox
            val parts = token.split("_")
            if (parts.isEmpty()) return Sandbox

            return when (parts[0].lowercase()) {
                "dev" -> Dev
                "staging" -> Staging
                "sandbox" -> Sandbox
                "cert" -> Cert
                "prod" -> Prod
                else -> Sandbox
            }
        }

        fun fromForageContext(context: ForageContext?): EnvConfig {
            return fromSessionToken(context?.sessionToken)
        }
    }
}

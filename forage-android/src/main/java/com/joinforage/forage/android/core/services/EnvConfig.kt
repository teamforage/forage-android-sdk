package com.joinforage.forage.android.core.services

import com.joinforage.forage.android.BuildConfig

internal enum class EnvOption(val value: String) {
    MOCK("mock"),
    LOCAL("local"),
    DEV("dev"),
    STAGING("staging"),
    SANDBOX("sandbox"),
    CERT("cert"),
    PROD("prod")
}

// Set when actually using a MockWebServer
internal var mockBaseUrl = "http://localhost"

internal sealed class EnvConfig(
    val FLAVOR: EnvOption,
    val apiBaseUrl: String,
    val vaultBaseUrl: String,
    val ddClientToken: String
) {
    // For the time being, I figure we can consume BuildConfig in exactly
    // one spot (here) to reduce the pieces of source code have to couple
    // to BuildConfig.
    val PUBLISH_VERSION: String = BuildConfig.PUBLISH_VERSION

    object Mock : EnvConfig(
        FLAVOR = EnvOption.MOCK,
        apiBaseUrl = mockBaseUrl,
        vaultBaseUrl = mockBaseUrl,
        ddClientToken = "pubf13cedf24ba2ad50d4b9cb0b0100bd4a"
    )

    object Local : EnvConfig(
        FLAVOR = EnvOption.LOCAL,
        apiBaseUrl = "http://10.0.2.2:8000/",
        vaultBaseUrl = "http://10.0.2.2:3999/",
        ddClientToken = "pubf13cedf24ba2ad50d4b9cb0b0100bd4a"
    )

    object Dev : EnvConfig(
        FLAVOR = EnvOption.DEV,
        apiBaseUrl = "https://api.dev.joinforage.app/",
        vaultBaseUrl = "https://vault.dev.joinforage.app/",
        ddClientToken = "pubf13cedf24ba2ad50d4b9cb0b0100bd4a"
    )

    object Staging : EnvConfig(
        FLAVOR = EnvOption.STAGING,
        apiBaseUrl = "https://api.staging.joinforage.app/",
        vaultBaseUrl = "https://vault.staging.joinforage.app/",
        ddClientToken = "pubf13cedf24ba2ad50d4b9cb0b0100bd4a"
    )

    object Sandbox : EnvConfig(
        FLAVOR = EnvOption.SANDBOX,
        apiBaseUrl = "https://api.sandbox.joinforage.app/",
        vaultBaseUrl = "https://vault.sandbox.joinforage.app/",
        ddClientToken = "pubf13cedf24ba2ad50d4b9cb0b0100bd4a"
    )

    object Cert : EnvConfig(
        FLAVOR = EnvOption.CERT,
        apiBaseUrl = "https://api.cert.joinforage.app/",
        vaultBaseUrl = "https://vault.cert.joinforage.app/",
        ddClientToken = "pubf13cedf24ba2ad50d4b9cb0b0100bd4a"
    )

    object Prod : EnvConfig(
        FLAVOR = EnvOption.PROD,
        apiBaseUrl = "https://api.joinforage.app/",
        vaultBaseUrl = "https://vault.joinforage.app/",
        ddClientToken = "pubf13cedf24ba2ad50d4b9cb0b0100bd4a"
    )

    companion object {
        fun fromSessionToken(sessionToken: String?): EnvConfig {
            val token = sessionToken?.takeIf { it.isNotEmpty() } ?: return Sandbox
            val parts = token.split("_")
            if (parts.isEmpty()) return Sandbox

            return when (parts[0].lowercase()) {
                "mock" -> Mock
                "local" -> Local
                "dev" -> Dev
                "staging" -> Staging
                "sandbox" -> Sandbox
                "cert" -> Cert
                "prod" -> Prod
                else -> Sandbox
            }
        }

        fun fromForageConfig(config: ForageConfig?): EnvConfig {
            return fromSessionToken(config?.sessionToken)
        }

        fun inProd(config: ForageConfig?): Boolean {
            return fromForageConfig(config).FLAVOR == EnvOption.PROD
        }
    }
}

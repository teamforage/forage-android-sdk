package com.joinforage.android.example.network.model

internal enum class EnvOption(val value: String) {
    Local("local"),
    DEV("dev"),
    STAGING("staging"),
    SANDBOX("sandbox"),
    CERT("cert"),
    PROD("prod")
}

internal sealed class EnvConfig(
    val FLAVOR: EnvOption,
    val baseUrl: String
) {

    object Local : EnvConfig(
        FLAVOR = EnvOption.Local,
        baseUrl = "http://10.0.2.2:8000/"
    )

    object Dev : EnvConfig(
        FLAVOR = EnvOption.DEV,
        baseUrl = "https://api.dev.joinforage.app/"
    )

    object Staging : EnvConfig(
        FLAVOR = EnvOption.STAGING,
        baseUrl = "https://api.staging.joinforage.app/"
    )

    object Sandbox : EnvConfig(
        FLAVOR = EnvOption.SANDBOX,
        baseUrl = "https://api.sandbox.joinforage.app/"
    )

    object Cert : EnvConfig(
        FLAVOR = EnvOption.CERT,
        baseUrl = "https://api.cert.joinforage.app/"
    )

    object Prod : EnvConfig(
        FLAVOR = EnvOption.PROD,
        baseUrl = "https://api.joinforage.app/"
    )

    companion object {
        fun fromSessionToken(sessionToken: String?): EnvConfig {
            val token = sessionToken?.takeIf { it.isNotEmpty() } ?: return Sandbox
            val parts = token.split("_")
            if (parts.isEmpty()) return Sandbox

            return when (parts[0].lowercase()) {
                "local" -> Local
                "dev" -> Dev
                "staging" -> Staging
                "sandbox" -> Sandbox
                "cert" -> Cert
                "prod" -> Prod
                else -> Sandbox
            }
        }
    }
}

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
    val baseUrl: Pair<String, String?>
) {

    object Local : EnvConfig(
        FLAVOR = EnvOption.Local,
        baseUrl = Pair("http://10.0.2.2/", "api.joinforage.localhost")
    )

    object Dev : EnvConfig(
        FLAVOR = EnvOption.DEV,
        baseUrl = Pair("https://api.dev.joinforage.app/", null)
    )

    object Staging : EnvConfig(
        FLAVOR = EnvOption.STAGING,
        baseUrl = Pair("https://api.staging.joinforage.app/", null)
    )

    object Sandbox : EnvConfig(
        FLAVOR = EnvOption.SANDBOX,
        baseUrl = Pair("https://api.sandbox.joinforage.app/", null)
    )

    object Cert : EnvConfig(
        FLAVOR = EnvOption.CERT,
        baseUrl = Pair("https://api.cert.joinforage.app/", null)
    )

    object Prod : EnvConfig(
        FLAVOR = EnvOption.PROD,
        baseUrl = Pair("https://api.joinforage.app/", null)
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

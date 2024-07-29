package com.joinforage.forage.android.core.services

/**
 * The configuration details that Forage needs to create a functional [ForageElement][com.joinforage.forage.android.core.ui.element.ForageElement].
 *
 * @property merchantId A unique Merchant ID that Forage provides during onboarding
 * onboarding, as in `123ab45c67`.
 * The Merchant ID can be found in the Forage [Sandbox](https://dashboard.sandbox.joinforage.app/login/)
 * or [Production](https://dashboard.joinforage.app/login/) Dashboard.
 *
 * @property sessionToken A short-lived token that authenticates front-end requests to Forage.
 * To create one, send a server-side `POST` request from your backend to the
 * [`/session_token/`](https://docs.joinforage.app/reference/create-session-token) endpoint.
 *
 * @constructor Creates an instance of the [ForageConfig] data class.
 */
data class ForageConfig(
    val merchantId: String,
    val sessionToken: String
) {
    internal val envConfig = EnvConfig.fromForageConfig(this)
}

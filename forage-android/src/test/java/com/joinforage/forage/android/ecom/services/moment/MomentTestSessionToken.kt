package com.joinforage.forage.android.ecom.services.moment

import com.joinforage.forage.android.mock.TestForagePaymentSheet.Companion.MOCK_SESSION_TOKEN

/**
 * The session token moment tests use: the usual test token, but for `dev` instead of `mock`.
 *
 * Creating a `ForageConfig` with a `mock_` token creates `EnvConfig.Mock`, which keeps whatever
 * `mockBaseUrl` is at that moment. If that happens before `ForageSDKTest` points `mockBaseUrl` at
 * its MockWebServer, its requests go to the wrong place and it fails.
 */
internal val MOMENT_TEST_SESSION_TOKEN = MOCK_SESSION_TOKEN.replaceFirst("mock_", "dev_")

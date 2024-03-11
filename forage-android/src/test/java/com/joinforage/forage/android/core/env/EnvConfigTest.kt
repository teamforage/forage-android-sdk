package com.joinforage.forage.android.core.env

import com.joinforage.forage.android.core.EnvConfig
import com.joinforage.forage.android.ui.ForageConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class EnvConfigTest_fromSessionToken {
    @Test
    fun `when passed null returns Sandbox`() {
        val result = EnvConfig.fromSessionToken(null)
        assertThat(result).isEqualTo(EnvConfig.Sandbox)
    }

    @Test
    fun `when passed string with no _ returns Sandbox`() {
        val result = EnvConfig.fromSessionToken("certtoken1010101")
        assertThat(result).isEqualTo(EnvConfig.Sandbox)
    }

    @Test
    fun `when parsed parts are empty string returns Sandbox`() {
        val result = EnvConfig.fromSessionToken("_")
        assertThat(result).isEqualTo(EnvConfig.Sandbox)
    }

    @Test
    fun `not case sensitive`() {
        val result = EnvConfig.fromSessionToken("StAgInG_202020202")
        assertThat(result).isEqualTo(EnvConfig.Staging)
    }

    @Test
    fun `correctly parses DEV tokens`() {
        val result = EnvConfig.fromSessionToken("dev_202020202")
        assertThat(result).isEqualTo(EnvConfig.Dev)
    }

    @Test
    fun `correctly parses STAGING tokens`() {
        val result = EnvConfig.fromSessionToken("staging_202020202")
        assertThat(result).isEqualTo(EnvConfig.Staging)
    }

    @Test
    fun `correctly parses SANDBOX tokens`() {
        val result = EnvConfig.fromSessionToken("sandbox_202020202")
        assertThat(result).isEqualTo(EnvConfig.Sandbox)
    }

    @Test
    fun `correctly parses CERT tokens`() {
        val result = EnvConfig.fromSessionToken("cert_202020202")
        assertThat(result).isEqualTo(EnvConfig.Cert)
    }

    @Test
    fun `correctly parses PROD tokens`() {
        val result = EnvConfig.fromSessionToken("prod_202020202")
        assertThat(result).isEqualTo(EnvConfig.Prod)
    }
}

class EnvConfigTest_fromForageConfig {
    @Test
    fun `when passed null returns Sandbox`() {
        val result = EnvConfig.fromForageConfig(null)
        assertThat(result).isEqualTo(EnvConfig.Sandbox)
    }

    @Test
    fun `when passed valid session token, it works`() {
        val result = EnvConfig.fromForageConfig(
            ForageConfig(
                merchantId = "",
                sessionToken = "dev_2020202"
            )
        )
        assertThat(result).isEqualTo(EnvConfig.Dev)
    }
}

class EnvConfigTest_BaseUrlsAreCorrect {
    @Test
    fun `test Dev baseUrl is correct`() {
        assertThat(EnvConfig.Dev.apiBaseUrl).isEqualTo("https://api.dev.joinforage.app/")
        assertThat(EnvConfig.Dev.vaultBaseUrl).isEqualTo("https://vault.dev.joinforage.app/proxy/")
    }

    @Test
    fun `test Staging baseUrl is correct`() {
        assertThat(EnvConfig.Staging.apiBaseUrl).isEqualTo("https://api.staging.joinforage.app/")
        assertThat(EnvConfig.Staging.vaultBaseUrl).isEqualTo("https://vault.staging.joinforage.app/proxy/")
    }

    @Test
    fun `test Sandbox baseUrl is correct`() {
        assertThat(EnvConfig.Sandbox.apiBaseUrl).isEqualTo("https://api.sandbox.joinforage.app/")
        assertThat(EnvConfig.Sandbox.vaultBaseUrl).isEqualTo("https://vault.sandbox.joinforage.app/proxy/")
    }

    @Test
    fun `test Cert baseUrl is correct`() {
        assertThat(EnvConfig.Cert.apiBaseUrl).isEqualTo("https://api.cert.joinforage.app/")
        assertThat(EnvConfig.Cert.vaultBaseUrl).isEqualTo("https://vault.cert.joinforage.app/proxy/")
    }

    @Test
    fun `test Prod baseUrl is correct`() {
        assertThat(EnvConfig.Prod.apiBaseUrl).isEqualTo("https://api.joinforage.app/")
        assertThat(EnvConfig.Prod.vaultBaseUrl).isEqualTo("https://vault.joinforage.app/proxy/")
    }
}

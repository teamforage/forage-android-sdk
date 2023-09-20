package com.joinforage.forage.android.core.env

import com.joinforage.forage.android.core.EnvConfig
import com.joinforage.forage.android.ui.ForageContext
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

class EnvConfigTest_fromForageContext {
    @Test
    fun `when passed null returns Sandbox`() {
        val result = EnvConfig.fromForageContext(null)
        assertThat(result).isEqualTo(EnvConfig.Sandbox)
    }

    @Test
    fun `when passed valid session token, it works`() {
        val result = EnvConfig.fromForageContext(
            ForageContext(
                merchantId = "",
                sessionToken = "dev_2020202"
            )
        )
        assertThat(result).isEqualTo(EnvConfig.Dev)
    }
}

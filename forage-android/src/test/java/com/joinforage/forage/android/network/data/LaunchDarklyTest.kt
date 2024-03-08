package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.ALWAYS_BT_PERCENT
import com.joinforage.forage.android.ALWAYS_VGS_PERCENT
import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.computeVaultType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

val LD_MOBILE_KEY = "some key"

@RunWith(RobolectricTestRunner::class)
class LaunchDarklyTest() {

    // NOTE: It is essential that the TestData source live in a
    // companion object. For some reason moving this variable
    // to be a instance variable instead of a static variable causes
    // race conditions within tests
    companion object {
        private val ldConfig = "anystringworks"
    }

    @Test
    fun `Test computeVaultType returns BT percent is 100f`() {
        assertThat(computeVaultType(ALWAYS_BT_PERCENT)).isEqualTo(VaultType.BT_VAULT_TYPE)
    }

    @Test
    fun `Test computeVaultType returns VGS percent is 0f`() {
        assertThat(computeVaultType(ALWAYS_VGS_PERCENT)).isEqualTo(VaultType.VGS_VAULT_TYPE)
    }

}

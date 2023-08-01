package com.joinforage.forage.android.network

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import com.joinforage.forage.android.LDFlags
import com.joinforage.forage.android.LDManager
import com.joinforage.forage.android.VaultConstants
import com.joinforage.forage.android.core.Log
import com.launchdarkly.sdk.LDValue
import com.launchdarkly.sdk.android.integrations.TestData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LaunchDarklyTest() {
    private val alwaysBT = 100.0
    private val alwaysVGS = 0.0

    companion object {
        private lateinit var td: TestData

        @BeforeClass @JvmStatic
        fun setup() {
            td = TestData.dataSource()
        }
    }

    @After
    fun resetForage() {
        LDManager.vaultType = null
    }

    @Test
    fun `The outcome should always be BT`() = runTest {
        // Set the test data to send all traffic to BT
        td.update(td.flag(LDFlags.VAULT_PRIMARY_TRAFFIC_PERCENTAGE_FLAG).variations(LDValue.of(alwaysBT)))
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val vaultType = LDManager.getVaultProvider(app, logger = Log.getSilentInstance(), dataSource = td)
        assertThat(vaultType).isEqualTo(VaultConstants.BT_VAULT_TYPE)

        // Update the test data to send all traffic to VGS
        // Since ForageSDK is a singleton, we should still return BT in this instance
        td.update(td.flag(LDFlags.VAULT_PRIMARY_TRAFFIC_PERCENTAGE_FLAG).variations(LDValue.of(alwaysVGS)))
        val secondVaultType = LDManager.getVaultProvider(app, logger = Log.getSilentInstance(), dataSource = td)
        assertThat(secondVaultType).isEqualTo(VaultConstants.BT_VAULT_TYPE)
    }

    @Test
    fun `The outcome should always be VGS`() = runTest {
        // Set the test data to send all traffic to VGS
        td.update(td.flag(LDFlags.VAULT_PRIMARY_TRAFFIC_PERCENTAGE_FLAG).variations(LDValue.of(alwaysVGS)))
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val vaultType = LDManager.getVaultProvider(app, logger = Log.getSilentInstance(), dataSource = td)
        assertThat(vaultType).isEqualTo(VaultConstants.VGS_VAULT_TYPE)

        // Update the test data to send all traffic to BT
        // Since ForageSDK is a singleton, we should still return VGS in this instance
        td.update(td.flag(LDFlags.VAULT_PRIMARY_TRAFFIC_PERCENTAGE_FLAG).variations(LDValue.of(alwaysBT)))
        val secondVaultType = LDManager.getVaultProvider(app, logger = Log.getSilentInstance(), dataSource = td)
        assertThat(secondVaultType).isEqualTo(VaultConstants.VGS_VAULT_TYPE)
    }
}

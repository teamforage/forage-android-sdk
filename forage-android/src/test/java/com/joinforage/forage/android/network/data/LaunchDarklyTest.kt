package com.joinforage.forage.android.network

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import com.joinforage.forage.android.ForageSDK
import com.joinforage.forage.android.LDConstants
import com.joinforage.forage.android.VaultConstants
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
    private val alwaysBT = 0.0
    private val alwaysVGS = 100.0

    companion object {
        private lateinit var td: TestData

        @BeforeClass @JvmStatic
        fun setup() {
            td = TestData.dataSource()
        }
    }

    @After
    fun resetForage() {
        ForageSDK.reset()
    }

    @Test
    fun `The outcome should always be BT`() = runTest {
        // Set the test data to send all traffic to BT
        td.update(td.flag(LDConstants.VAULT_TYPE_FLAG).variations(LDValue.of(alwaysBT)))
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val vaultType = ForageSDK.getVaultProvider(app, td)
        assertThat(vaultType).isEqualTo(VaultConstants.BT_VAULT_TYPE)

        // Update the test data to send all traffic to VGS
        // Since ForageSDK is a singleton, we should still return BT in this instance
        val secondTd = TestData.dataSource()
        secondTd.update(secondTd.flag(LDConstants.VAULT_TYPE_FLAG).variations(LDValue.of(alwaysVGS)))
        val secondVaultType = ForageSDK.getVaultProvider(app, td)
        assertThat(secondVaultType).isEqualTo(VaultConstants.BT_VAULT_TYPE)
    }

    @Test
    fun `The outcome should always be VGS`() = runTest {
        // Set the test data to send all traffic to VGS
        td.update(td.flag(LDConstants.VAULT_TYPE_FLAG).variations(LDValue.of(alwaysVGS)))
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val vaultType = ForageSDK.getVaultProvider(app, td)
        assertThat(vaultType).isEqualTo(VaultConstants.VGS_VAULT_TYPE)

        // Update the test data to send all traffic to BT
        // Since ForageSDK is a singleton, we should still return VGS in this instance
        val secondTd = TestData.dataSource()
        secondTd.update(secondTd.flag(LDConstants.VAULT_TYPE_FLAG).variations(LDValue.of(alwaysBT)))
        val secondVaultType = ForageSDK.getVaultProvider(app, td)
        assertThat(secondVaultType).isEqualTo(VaultConstants.VGS_VAULT_TYPE)
    }
}

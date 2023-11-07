package com.joinforage.forage.android.network.data

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import com.joinforage.forage.android.ALWAYS_BT
import com.joinforage.forage.android.ALWAYS_VGS
import com.joinforage.forage.android.LDFlags
import com.joinforage.forage.android.LDManager
import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.computeVaultType
import com.joinforage.forage.android.core.telemetry.Log
import com.launchdarkly.sdk.LDValue
import com.launchdarkly.sdk.android.integrations.TestData
import kotlinx.coroutines.test.runTest
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
        private var td: TestData = TestData.dataSource()
    }

    @Test
    fun `Test computeVaultType returns BT percent is 100f`() {
        assertThat(computeVaultType(ALWAYS_BT)).isEqualTo(VaultType.BT_VAULT_TYPE)
    }

    @Test
    fun `Test computeVaultType returns VGS percent is 0f`() {
        assertThat(computeVaultType(ALWAYS_VGS)).isEqualTo(VaultType.VGS_VAULT_TYPE)
    }

    @Test
    fun `It should default to using VGS as the vault provider`() = runTest {
        // set up LDManager, importantly, we're not giving it any value for
        // primaryTrafficPercent since we want to test it defaults to VGS
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val ldConfig = LDManager.TEST_createLdConfig(LD_MOBILE_KEY, td)
        LDManager.initialize(app, logger = Log.getSilentInstance(), ldConfig)

        // we don't want to use the cached value from another test
        // since LDManager is a singleton
        LDManager.TEST_clearPrimaryTrafficPercentCache()

        // it should default to using VGS as the vault provider
        assertThat(LDManager.getVaultProvider()).isEqualTo(VaultType.VGS_VAULT_TYPE)
    }

    @Test
    fun `It should cache primaryTrafficPercent flag for subsequent calls to getVaultProvider`() = runTest {
        // set up LDManager
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val ldConfig = LDManager.TEST_createLdConfig(LD_MOBILE_KEY, td)
        LDManager.initialize(app, logger = Log.getSilentInstance(), ldConfig)

        // we don't want to use the cached value from another test
        // since LDManager is a singleton
        LDManager.TEST_clearPrimaryTrafficPercentCache()

        // Set the test data to send all traffic to BT
        td.update(td.flag(LDFlags.VAULT_PRIMARY_TRAFFIC_PERCENTAGE_FLAG).variations(LDValue.of(ALWAYS_BT)))

        // it should consume the flag and choose BT
        val original = LDManager.getVaultProvider()
        assertThat(original).isEqualTo(VaultType.BT_VAULT_TYPE)

        // Update the test data to send all traffic to VGS
        td.update(td.flag(LDFlags.VAULT_PRIMARY_TRAFFIC_PERCENTAGE_FLAG).variations(LDValue.of(ALWAYS_VGS)))

        // we expect the value to be cached and thus should be BT again
        val cached = LDManager.getVaultProvider()
        assertThat(cached).isEqualTo(VaultType.BT_VAULT_TYPE)
    }

    @Test
    fun `Default polling intervals`() = runTest {
        // set up LDManager
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val ldConfig = LDManager.TEST_createLdConfig(LD_MOBILE_KEY, td)
        LDManager.initialize(app, logger = Log.getSilentInstance(), ldConfig)

        // Set the test data to be {"intervals" : [1000]}
        td.update(td.flag(LDFlags.ISO_POLLING_WAIT_INTERVALS).variations(LDValue.buildObject().put("intervals", LDValue.Convert.Long.arrayFrom(List(1) { 1000L })).build()))

        val pollingIntervals = LDManager.getPollingIntervals()
        assertThat(pollingIntervals).isEqualTo(longArrayOf(1000L))
    }
}

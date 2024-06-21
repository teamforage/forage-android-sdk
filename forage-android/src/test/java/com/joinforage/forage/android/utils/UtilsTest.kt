package com.joinforage.forage.android.utils

import com.joinforage.forage.android.core.services.getJitterAmount
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.random.Random

class UtilsTest {
    @Test
    fun `Jitter value should be in range -25 to 25`() = runTest {
        val fixedRandomZero = Random(13)
        val jitterZero = getJitterAmount(fixedRandomZero)
        assertThat(jitterZero).isEqualTo(0)

        val fixedRandomMin = Random(16)
        val jitterMin = getJitterAmount(fixedRandomMin)
        assertThat(jitterMin).isEqualTo(-25)

        val fixedRandomMax = Random(60)
        val jitterMax = getJitterAmount(fixedRandomMax)
        assertThat(jitterMax).isEqualTo(25)
    }
}

package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.mock.MockLogger
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.vault.BTPinCollector
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BTPinCollectorTest() : MockServerSuite() {
    @Test
    fun `it should return the invalid PIN error`() = runTest {
        val mockLogger = MockLogger()

        val expectedResponse = ForageApiResponse.Failure(
            listOf(ForageError(400, "user_error", "Invalid EBT Card PIN entered. Please enter your 4-digit PIN."))
        )

        val response = BTPinCollector.returnIncompletePinError(emptyMap(), mockLogger)

        assertThat(response).isEqualTo(expectedResponse)
        // THIS IS JUST A CHECK TO ENSURE THAT MOCK LOGGER WAS CALLED!
        assertThat(mockLogger.warnLogs.count()).isEqualTo(1)
    }
}

package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.collect.VGSPinCollector
import com.joinforage.forage.android.core.telemetry.metrics.TestResponseMonitor
import com.joinforage.forage.android.mock.MockLogger
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.verygoodsecurity.vgscollect.core.model.network.VGSResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal class MockContinuation : Continuation<ForageApiResponse<String>> {
    var savedResult: Result<ForageApiResponse<String>>? = null
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<ForageApiResponse<String>>) {
        savedResult = result
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class VGSPinCollectorTest() : MockServerSuite() {
    @Test
    fun `it should return the unknown error`() = runTest {
        val mockLogger = MockLogger()
        val mockResponseMonitor = TestResponseMonitor(mockLogger)
        val mockContinuation = MockContinuation()
        VGSPinCollector.returnUnknownError(mockResponseMonitor, mockContinuation)

        val expectedResponse = Result.success(
            ForageApiResponse.Failure(
                listOf(ForageError(500, "unknown_server_error", "Unknown Server Error"))
            )
        )

        assertThat(mockContinuation.savedResult).isEqualTo(expectedResponse)
        // THIS IS JUST A CHECK TO ENSURE THAT MOCK LOGGER WAS CALLED!
        assertThat(mockLogger.errorLogs.count()).isEqualTo(1)
    }

    @Test
    fun `it should return the vgs error`() = runTest {
        val mockLogger = MockLogger()
        val mockResponseMonitor = TestResponseMonitor(mockLogger)
        val mockContinuation = MockContinuation()

        val responseCode = 404
        val expectedResponse = Result.success(
            ForageApiResponse.Failure(
                listOf(ForageError(responseCode, "unknown_server_error", "Unknown Server Error"))
            )
        )

        VGSPinCollector.returnVgsError(VGSResponse.ErrorResponse(errorCode = responseCode), mockResponseMonitor, mockContinuation)

        assertThat(mockContinuation.savedResult).isEqualTo(expectedResponse)
        // THIS IS JUST A CHECK TO ENSURE THAT MOCK LOGGER WAS CALLED!
        assertThat(mockLogger.errorLogs.count()).isEqualTo(1)
    }

    @Test
    fun `it should return the invalid PIN error`() = runTest {
        val mockLogger = MockLogger()
        val mockContinuation = MockContinuation()

        val expectedResponse = Result.success(
            ForageApiResponse.Failure(
                listOf(ForageError(400, "user_error", "Invalid EBT Card PIN entered. Please enter your 4-digit PIN."))
            )
        )

        VGSPinCollector.returnIncompletePinError(emptyMap(), mockContinuation, mockLogger)

        assertThat(mockContinuation.savedResult).isEqualTo(expectedResponse)
        // THIS IS JUST A CHECK TO ENSURE THAT MOCK LOGGER WAS CALLED!
        assertThat(mockLogger.warnLogs.count()).isEqualTo(1)
    }
    // TODO: Test the Forage Error function!
}

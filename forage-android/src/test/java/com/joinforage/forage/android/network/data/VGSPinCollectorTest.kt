package com.joinforage.forage.android.network.data

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonObject
import com.joinforage.forage.android.collect.VGSPinCollector
import com.joinforage.forage.android.core.telemetry.NetworkMonitor
import com.joinforage.forage.android.core.telemetry.metrics.MockLogger
import com.joinforage.forage.android.core.telemetry.metrics.TestResponseMonitor
import com.joinforage.forage.android.fixtures.givenEncryptionKey
import com.joinforage.forage.android.fixtures.givenPaymentMethodRef
import com.joinforage.forage.android.fixtures.givenPaymentRef
import com.joinforage.forage.android.fixtures.returnsEncryptionKeySuccessfully
import com.joinforage.forage.android.fixtures.returnsFailedPayment
import com.joinforage.forage.android.network.model.ForageApiError
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.network.model.ForageErrorObj
import com.joinforage.forage.android.ui.ForagePINEditText
import com.verygoodsecurity.vgscollect.core.model.network.VGSRequest
import com.verygoodsecurity.vgscollect.core.model.network.VGSResponse
import org.assertj.core.api.Assertions.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal class MockContinuation: Continuation<ForageApiResponse<String>> {
    var savedResult: Result<ForageApiResponse<String>>? = null
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<ForageApiResponse<String>>) {
        savedResult = result
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class VGSPinCollectorTest() : MockServerSuite() {
    private var vgsPinCollector: VGSPinCollector? = null
    @Before
    override fun setup() {
        super.setup()
//        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
//        vgsPinCollector = VGSPinCollector(app, ForagePINEditText(app), "1234567")
    }

    @Test
    fun `it should return the unknown error`() = runTest {
        val mockLogger = MockLogger()
        val mockResponseMonitor = TestResponseMonitor(mockLogger)
        val mockContinuation = MockContinuation()
        VGSPinCollector.returnUnknownError(mockResponseMonitor, mockContinuation)

        val expectedResponse = Result.success(
            ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Unknown Server Error"))
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
            ForageApiResponse.Failure(listOf(ForageError(responseCode, "unknown_server_error", "Unknown Server Error"))
            )
        )

        VGSPinCollector.returnVgsError(VGSResponse.ErrorResponse(errorCode = responseCode), mockResponseMonitor, mockContinuation)

        assertThat(mockContinuation.savedResult).isEqualTo(expectedResponse)
        // THIS IS JUST A CHECK TO ENSURE THAT MOCK LOGGER WAS CALLED!
        assertThat(mockLogger.errorLogs.count()).isEqualTo(1)
    }


}

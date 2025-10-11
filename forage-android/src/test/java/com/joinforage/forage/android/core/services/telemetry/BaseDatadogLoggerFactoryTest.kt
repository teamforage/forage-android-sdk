package com.joinforage.forage.android.core.services.telemetry

import android.content.Context
import com.joinforage.datadog.android.Datadog
import com.joinforage.datadog.android.api.SdkCore
import com.joinforage.datadog.android.log.Logs
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.telemetry.BaseDatadogLoggerFactory.Companion.FORAGE_DATADOG_INSTANCE_NAME
import com.joinforage.forage.android.core.services.telemetry.BaseDatadogLoggerFactory.Companion.NO_OP_INSTANCE_NAME
import com.joinforage.forage.android.mock.TestForagePaymentSheet.Companion.MOCK_SESSION_TOKEN
import com.joinforage.forage.android.mock.TestForagePaymentSheet.Companion.TEST_MERCHANT_ID
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class BaseDatadogLoggerFactoryTest {
    lateinit var datadogMock: MockedStatic<Datadog>
    lateinit var logsMock: MockedStatic<Logs>
    lateinit var functionalSdkCore: SdkCore
    lateinit var noopSdkCore: SdkCore
    internal lateinit var loggerFactory: BaseDatadogLoggerFactory

    @Before
    fun before() {
        datadogMock = Mockito.mockStatic(Datadog::class.java)
        functionalSdkCore = mock(SdkCore::class.java)
        noopSdkCore = mock(SdkCore::class.java)

        val forageConfig = ForageConfig(TEST_MERCHANT_ID, MOCK_SESSION_TOKEN)
        loggerFactory = object :
            BaseDatadogLoggerFactory(
                mock(Context::class.java),
                forageConfig,
                LogService.Ecom,
                LogAttributes(
                    forageConfig = forageConfig,
                    traceId = "trace-id",
                    customerId = "customer-id"
                ),
                "any-prefix"
            ) {}

        logsMock = Mockito.mockStatic(Logs::class.java)
        whenever(Logs.enable(any(), any())).then {}
    }

    @After
    fun after() {
        datadogMock.close()
        logsMock.close()
    }

    @Test
    fun `verify successful initialization`() {
        whenever(Datadog.isInitialized(eq(FORAGE_DATADOG_INSTANCE_NAME))).thenReturn(false)
        whenever(Datadog.initialize(eq(FORAGE_DATADOG_INSTANCE_NAME), any(), any(), any())).thenReturn(functionalSdkCore)

        val actualSdkCore = loggerFactory.getSdkCore()
        assertEquals(actualSdkCore, functionalSdkCore)

        datadogMock.verify { Datadog.isInitialized(eq(FORAGE_DATADOG_INSTANCE_NAME)) }
        datadogMock.verify({ Datadog.getInstance(eq(FORAGE_DATADOG_INSTANCE_NAME)) }, never())
        datadogMock.verify { Datadog.initialize(eq(FORAGE_DATADOG_INSTANCE_NAME), any(), any(), any()) }
        datadogMock.verify({ Datadog.getInstance(eq(NO_OP_INSTANCE_NAME)) }, never())
    }

    @Test
    fun `verify fallback to no-op logger`() {
        whenever(Datadog.isInitialized(eq(FORAGE_DATADOG_INSTANCE_NAME))).thenReturn(false)
        whenever(Datadog.initialize(eq(FORAGE_DATADOG_INSTANCE_NAME), any(), any(), any())).thenReturn(null)
        whenever(Datadog.getInstance(eq(NO_OP_INSTANCE_NAME))).thenReturn(noopSdkCore)

        val actualSdkCore = loggerFactory.getSdkCore()
        assertEquals(actualSdkCore, noopSdkCore)

        datadogMock.verify { Datadog.isInitialized(eq(FORAGE_DATADOG_INSTANCE_NAME)) }
        datadogMock.verify({ Datadog.getInstance(eq(FORAGE_DATADOG_INSTANCE_NAME)) }, never())
        datadogMock.verify { Datadog.initialize(eq(FORAGE_DATADOG_INSTANCE_NAME), any(), any(), any()) }
        datadogMock.verify { Datadog.getInstance(eq(NO_OP_INSTANCE_NAME)) }
    }

    @Test
    fun `verify already initialized`() {
        whenever(Datadog.isInitialized(eq(FORAGE_DATADOG_INSTANCE_NAME))).thenReturn(true)
        whenever(Datadog.getInstance(eq(FORAGE_DATADOG_INSTANCE_NAME))).thenReturn(functionalSdkCore)

        val actualSdkCore = loggerFactory.getSdkCore()
        assertEquals(actualSdkCore, functionalSdkCore)

        datadogMock.verify { Datadog.isInitialized(eq(FORAGE_DATADOG_INSTANCE_NAME)) }
        datadogMock.verify { Datadog.getInstance(eq(FORAGE_DATADOG_INSTANCE_NAME)) }
        datadogMock.verify({ Datadog.initialize(any(), any(), any(), any()) }, never())
    }
}

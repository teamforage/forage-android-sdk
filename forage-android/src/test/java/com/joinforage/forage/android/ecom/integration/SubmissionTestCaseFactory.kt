package com.joinforage.forage.android.ecom.integration

import com.joinforage.forage.android.core.logger.InMemoryLogger
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.engine.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.requests.ClientApiRequest
import com.joinforage.forage.android.core.services.forageapi.requests.Headers
import com.joinforage.forage.android.core.services.forageapi.requests.makeApiUrl
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.vault.ISecurePinCollector
import com.joinforage.forage.android.core.services.vault.RosettaPinSubmitter
import com.joinforage.forage.android.core.services.vault.errors.BaseErrorStrategy
import com.joinforage.forage.android.core.services.vault.requests.ISubmitRequestBuilder
import com.joinforage.forage.android.core.services.vault.submission.PinSubmission
import com.joinforage.forage.android.ecom.TestStringResponseHttpEngine
import com.joinforage.forage.android.ecom.services.DaggerForageSDKTestComponent
import org.json.JSONObject

internal class TestableSecurePinCollector(
    private val pin: String,
    private val isComplete: Boolean = true
) : ISecurePinCollector {
    var wasCleared = false
    override fun getPin(): String {
        return pin
    }

    override fun clearText() {
        wasCleared = true
    }

    override fun isComplete(): Boolean {
        return isComplete
    }
}

internal data class SubmissionTestCase<T>(
    val submission: T,
    val logger: InMemoryLogger,
    val collector: TestableSecurePinCollector
)

internal class SubmissionTestCaseFactory(
    private val forageConfig: ForageConfig,
    private val vaultHttpEngine: IHttpEngine = TestStringResponseHttpEngine("yay, success!")
) {
    fun newPinSubmissionAttempt(
        pin: String,
        vaultHttpEngine: IHttpEngine = this.vaultHttpEngine,
        isComplete: Boolean = true
    ): SubmissionTestCase<PinSubmission> {
        val pinCollector = TestableSecurePinCollector(pin, isComplete = isComplete)
        val component = DaggerForageSDKTestComponent.builder()
            .forageConfig(forageConfig)
            .securePinCollector(pinCollector)
            .vaultHttpEngineOverride(vaultHttpEngine)
            .build()
        val pinSubmissionFactory = component.getPinSubmissionFactory()
        val logger = component.getLogger()
        val submission = pinSubmissionFactory.build(
            errorStrategy = BaseErrorStrategy(logger),
            requestBuilder = object : ISubmitRequestBuilder {
                override suspend fun buildRequest(
                    traceId: String,
                    vaultSubmitter: RosettaPinSubmitter
                ) = object : ClientApiRequest.PostRequest(
                    url = makeApiUrl(forageConfig, "api/payments/"),
                    forageConfig = forageConfig,
                    traceId = traceId,
                    apiVersion = Headers.ApiVersion.V_DEFAULT,
                    headers = Headers(idempotencyKey = "does not matter"),
                    body = JSONObject()
                ) {}
            },
            userAction = UserAction.BALANCE
        )
        return SubmissionTestCase(submission, logger as InMemoryLogger, pinCollector)
    }
}

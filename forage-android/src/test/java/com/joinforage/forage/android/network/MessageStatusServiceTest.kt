package com.joinforage.forage.android.network

import com.joinforage.forage.android.fixtures.givenContentId
import com.joinforage.forage.android.fixtures.returnsMessageCompletedSuccessfully
import com.joinforage.forage.android.fixtures.returnsUnauthorized
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.network.model.Message
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.Method
import me.jorgecastillo.hiroaki.headers
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import me.jorgecastillo.hiroaki.matchers.times
import me.jorgecastillo.hiroaki.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessageStatusServiceTest : MockServerSuite() {
    private lateinit var messageStatusService: MessageStatusService

    @Before
    override fun setup() {
        super.setup()

        messageStatusService = MessageStatusService(
            okHttpClient = OkHttpClientBuilder.provideOkHttpClient(BEARER_TOKEN, MERCHANT_ACCOUNT),
            httpUrl = server.url("")
        )
    }

    @Test
    fun `it should send the correct headers to get the message status`() = runTest {
        val contentId = "d789c086-9c4f-41c3-854a-1c436eee1d63"
        server.givenContentId(contentId).returnsMessageCompletedSuccessfully()

        messageStatusService.getStatus(contentId)

        server.verify("api/message/$contentId").called(
            times = times(1),
            method = Method.GET,
            headers = headers(
                "Authorization" to "Bearer $BEARER_TOKEN",
                "Merchant-Account" to MERCHANT_ACCOUNT
            )
        )
    }

    @Test
    fun `it should respond with a message when successful`() = runTest {
        val contentId = "d789c086-9c4f-41c3-854a-1c436eee1d63"
        server.givenContentId(contentId).returnsMessageCompletedSuccessfully()

        val response = messageStatusService.getStatus(contentId)

        val expectedResponse = Message(
            contentId = contentId,
            messageType = "0200",
            status = "completed",
            failed = false,
            errors = emptyList()
        )

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Success::class.java)
        val successResponse = response as ForageApiResponse.Success

        val balanceMessage = Message.ModelMapper.from(successResponse.data)
        assertThat(balanceMessage).isEqualTo(expectedResponse)
    }

    @Test
    fun `it should respond with an error when unable to get a Message`() = runTest {
        val contentId = "d789c086-9c4f-41c3-854a-1c436eee1d63"
        server.givenContentId(contentId).returnsUnauthorized()

        val messageResponse = messageStatusService.getStatus(contentId)

        assertThat(messageResponse).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)

        val response = messageResponse as ForageApiResponse.Failure

        assertThat(response.errors[0]).isEqualTo(ForageError(401, "missing_merchant_account", "No merchant account FNS number was provided."))
    }

    companion object {
        private const val BEARER_TOKEN: String = "AbCaccesstokenXyz"
        private const val MERCHANT_ACCOUNT: String = "1234567"
    }
}

package com.joinforage.forage.android.network

import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeyService
import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeys
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.OkHttpClientBuilder
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.fixtures.givenEncryptionKey
import com.joinforage.forage.android.fixtures.returnsEncryptionKeySuccessfully
import com.joinforage.forage.android.fixtures.returnsUnauthorizedEncryptionKey
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
class EncryptionKeyServiceTest : MockServerSuite() {
    private val bearerToken: String = "AbCaccesstokenXyz"
    private lateinit var encryptionKeyService: EncryptionKeyService

    @Before
    override fun setup() {
        super.setup()

        encryptionKeyService = EncryptionKeyService(
            okHttpClient = OkHttpClientBuilder.provideOkHttpClient(bearerToken),
            httpUrl = server.url("").toUrl().toString(),
            logger = Log.getSilentInstance()
        )
    }

    @Test
    fun `it should send the correct headers to get the encryption key`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()

        encryptionKeyService.getEncryptionKey()

        server.verify("iso_server/encryption_alias").called(
            times = times(1),
            method = Method.GET,
            headers = headers(
                "Authorization" to "Bearer $bearerToken"
            )
        )
    }

    @Test
    fun `it should return a success when the correct headers to get the encryption key are provided`() =
        runTest {
            server.givenEncryptionKey().returnsEncryptionKeySuccessfully()

            val response = encryptionKeyService.getEncryptionKey()

            assertThat(response).isExactlyInstanceOf(ForageApiResponse.Success::class.java)
            val successResponse = response as ForageApiResponse.Success

            val encryptionKey = EncryptionKeys.ModelMapper.from(successResponse.data)
            assertThat(encryptionKey).isEqualTo(EncryptionKeys(vgsAlias = "tok_sandbox_eZeWfkq1AkqYdiAJC8iweE", btAlias = "fake-bt-alias"))
        }

    @Test
    fun `it should return a failure when auth headers are not provided`() = runTest {
        server.givenEncryptionKey().returnsUnauthorizedEncryptionKey()

        val response = encryptionKeyService.getEncryptionKey()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val clientError = response as ForageApiResponse.Failure
        val expectedDetail = "Authentication credentials were not provided."

        assertThat(clientError.error.message).isEqualTo(expectedDetail)
    }
}

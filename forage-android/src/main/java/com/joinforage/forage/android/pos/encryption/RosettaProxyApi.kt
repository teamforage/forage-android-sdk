package com.joinforage.forage.android.pos.encryption

import com.joinforage.forage.android.core.EnvConfig
import com.joinforage.forage.android.pos.PosForageConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

// REQUESTS:

@JsonClass(generateAdapter = true)
internal data class CertificateSigningRequest(
    @Json(name = "csr") val csr: String
)

@JsonClass(generateAdapter = true)
internal data class InitializePosRequest(
    @Json(name = "certificate") val base64PublicKeyPEM: String
)

// RESPONSES:

/**
 * The Android client, for the time being, assumes that
 * Rosetta Proxy will return a JSON object with the
 * fields below AND that each field will be encrypted
 * with the public RSA key. So, for the Android client
 * to make use of each and any response field, it needs
 * to decrypt each field one at a time.
 */
@JsonClass(generateAdapter = true)
internal data class InitializePosResponse(
    // encrypted "intermediate pin encryption key"
    @Json(name = "encrypted_ipek") val base64EncryptedIpek: String,
    @Json(name = "cksum") val checksum: String,
    @Json(name = "cksum_algo") val checksumAlgorithm: String, // ex: CMAC
    @Json(name = "ksn") val keySerialNumber: String
)

@JsonClass(generateAdapter = true)
internal data class CertificateSigningResponse(
    @Json(name = "certificate") val signedCertificate: String
)

internal interface RosettaProxyApi {
    @POST("api/terminal/certificate/")
    suspend fun signCertificate(
        @Body csr: CertificateSigningRequest
    ): CertificateSigningResponse

    @POST("api/terminal/initialize/")
    suspend fun initializePos(
        @Body initParams: InitializePosRequest
    ): InitializePosResponse

    companion object {
        internal fun from(posTerminalId: String, posForageConfig: PosForageConfig): RosettaProxyApi {
            val commonHeadersInterceptor = Interceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${posForageConfig.sessionToken}")
                    .addHeader("Merchant-Account", posForageConfig.merchantId)
                    .addHeader("X-TERMINAL-ID", posTerminalId)
                    .build()
                chain.proceed(newRequest)
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(commonHeadersInterceptor)
                .build()

            val env = EnvConfig.fromSessionToken(posForageConfig.sessionToken)
            val retrofit = Retrofit.Builder()
                .baseUrl(env.vaultBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            return retrofit.create(RosettaProxyApi::class.java)
        }
    }
}

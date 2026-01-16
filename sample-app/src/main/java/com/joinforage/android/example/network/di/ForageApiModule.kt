package com.joinforage.android.example.network.di

import com.joinforage.android.example.network.ForageApi
import com.joinforage.android.example.network.interceptors.AuthInterceptor
import com.joinforage.android.example.network.model.EnvConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.addAdapter
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@ExperimentalStdlibApi
class ForageApiModule {

    private fun provideRetrofit(baseUrl: String): Retrofit {
        val authInterceptor = AuthInterceptor()
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()

        val moshi = Moshi.Builder()
            .addAdapter(Rfc3339DateJsonAdapter().nullSafe())
            .build()

        val moshiConverterFactory = MoshiConverterFactory.create(moshi)

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(moshiConverterFactory)
            .build()
    }

    fun provideForageApi(
        sessionToken: String
    ): ForageApi {
        val baseUrl = EnvConfig.fromSessionToken(sessionToken).baseUrl
        return provideRetrofit(baseUrl).create(ForageApi::class.java)
    }
}

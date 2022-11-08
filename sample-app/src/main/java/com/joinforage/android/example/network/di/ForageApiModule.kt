package com.joinforage.android.example.network.di

import com.joinforage.android.example.network.ForageApi
import com.joinforage.android.example.network.interceptors.AuthInterceptor
import com.skydoves.sandwich.adapters.ApiResponseCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.addAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@ExperimentalStdlibApi
@Module
@InstallIn(SingletonComponent::class)
object ForageApiModule {
    @Provides
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addAdapter(Rfc3339DateJsonAdapter().nullSafe())
        .add(KotlinJsonAdapterFactory())
        .build()

    private fun provideMoshiConverterFactory(): MoshiConverterFactory =
        MoshiConverterFactory.create(
            provideMoshi()
        )

    private fun provideRetrofit(
        httpClient: OkHttpClient
    ) = Retrofit.Builder()
        .baseUrl("https://api.sandbox.joinforage.app/")
        .client(httpClient)
        .addConverterFactory(provideMoshiConverterFactory())
        .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
        .build()

    @Singleton
    @Provides
    fun provideForageApi(
        httpClientBuilder: OkHttpClient.Builder,
        authInterceptor: AuthInterceptor
    ): ForageApi = provideRetrofit(
        httpClientBuilder
            .addInterceptor(authInterceptor)
            .build()
    ).create(ForageApi::class.java)
}

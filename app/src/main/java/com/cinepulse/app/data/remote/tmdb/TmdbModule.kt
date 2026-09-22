// data/remote/tmdb/TmdbModule.kt
package com.cinepulse.app.data.remote.tmdb

import com.cinepulse.app.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object TmdbModule {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    fun create(): TmdbApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${BuildConfig.TMDB_TOKEN}")  // v4 token
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TmdbApi::class.java)
    }
}
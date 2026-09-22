@file:OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
package com.cinepulse.app.data.remote.tmdb

// data/remote/tmdb/TmdbApi.kt
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class TmdbSearchResponse(
    val page: Int,
    val results: List<TmdbMedia>,
    @SerialName("total_pages") val totalPages: Int
)

@Serializable
data class TmdbMedia(
    val id: Int,
    val title: String? = null,
    val name: String? = null,             // TV uses `name`
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    val overview: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null
) {
    val displayTitle: String get() = title ?: name ?: "Unknown"
    val isTv: Boolean get() = mediaType == "tv" || (name != null && title == null)
}

@Serializable
data class TmdbWatchProvidersResponse(
    val id: Int,
    val results: Map<String, TmdbRegionProviders> = emptyMap()
)

@Serializable
data class TmdbRegionProviders(
    val link: String? = null,
    val flatrate: List<TmdbProvider>? = null,
    val rent: List<TmdbProvider>? = null,
    val buy: List<TmdbProvider>? = null
)

@Serializable
data class TmdbProvider(
    @SerialName("provider_id") val providerId: Int,
    @SerialName("provider_name") val providerName: String,
    @SerialName("logo_path") val logoPath: String?
)

interface TmdbApi {
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("trending/all/day")
    suspend fun trending(
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/{id}")
    suspend fun movieDetail(@Path("id") id: Int): TmdbMedia

    @GET("tv/{id}")
    suspend fun tvDetail(@Path("id") id: Int): TmdbMedia

    @GET("movie/{id}/watch/providers")
    suspend fun movieProviders(@Path("id") id: Int): TmdbWatchProvidersResponse

    @GET("tv/{id}/watch/providers")
    suspend fun tvProviders(@Path("id") id: Int): TmdbWatchProvidersResponse
}

package com.example.cinepulse

import org.json.JSONObject
import java.io.Serializable

/**
 * A movie OR a TV series from TMDB.
 * mediaType is either "movie" or "tv" (series).
 */
data class Movie(
    val id: Int,
    val mediaType: String,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String,
    val voteAverage: Double
) : Serializable {

    /** Unique key used in SQLite and Firestore, e.g. "movie_550" or "tv_1399" */
    val key: String get() = "${mediaType}_$id"

    val posterUrl: String? get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
    val backdropUrl: String? get() = (backdropPath ?: posterPath)?.let { "https://image.tmdb.org/t/p/w780$it" }
    val year: String get() = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else ""
    val isSeries: Boolean get() = mediaType == "tv"

    /** Convert to JSON so the movie can be stored in SQLite / Firestore as one string */
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("media_type", mediaType)
        put("title", title)
        put("overview", overview)
        put("poster_path", posterPath ?: "")
        put("backdrop_path", backdropPath ?: "")
        put("release_date", releaseDate)
        put("vote_average", voteAverage)
    }

    companion object {
        /**
         * Builds a Movie from a TMDB (or our own stored) JSON object.
         * TMDB uses "title"/"release_date" for movies and "name"/"first_air_date" for series.
         */
        fun fromJson(o: JSONObject, defaultType: String = "movie"): Movie {
            val type = o.optString("media_type", defaultType)
            val title = o.optString("title").ifEmpty { o.optString("name") }
            val date = o.optString("release_date").ifEmpty { o.optString("first_air_date") }
            fun path(name: String): String? =
                o.optString(name).takeIf { it.isNotEmpty() && it != "null" }
            return Movie(
                id = o.optInt("id"),
                mediaType = if (type == "tv") "tv" else "movie",
                title = title,
                overview = o.optString("overview"),
                posterPath = path("poster_path"),
                backdropPath = path("backdrop_path"),
                releaseDate = date,
                voteAverage = o.optDouble("vote_average", 0.0)
            )
        }
    }
}

/** One row of the user's watchlist. pending: 0 = synced, 1 = needs upload, 2 = needs delete */
data class WatchItem(
    val movie: Movie,
    val watched: Boolean,
    val archived: Boolean,
    val addedAt: Long,
    val pending: Int = 0
)

/** A comment shown on the detail screen. pending = saved offline, not posted yet */
data class Comment(
    val userName: String,
    val text: String,
    val createdAt: Long,
    val pending: Boolean = false
)

/** A community recommendation ("Thabo recommends Inception") */
data class Recommendation(
    val uid: String,
    val movie: Movie,
    val userName: String,
    val note: String,
    val createdAt: Long
)

/** An action done offline that still has to be sent to Firestore ("comment" or "recommend") */
data class PendingOp(
    val id: Long,
    val uid: String,
    val type: String,
    val movie: Movie,
    val text: String,
    val createdAt: Long
)

data class Details(val genres: String, val tagline: String)

data class CommunityRating(val average: Double, val count: Int, val mine: Float?)
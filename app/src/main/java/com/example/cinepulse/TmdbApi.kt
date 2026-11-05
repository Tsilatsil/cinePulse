package com.example.cinepulse

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

/**
 * Talks to the TMDB REST API (https://developer.themoviedb.org) using Volley.
 * Every list response is cached in SQLite, so if the phone is offline we show the last copy.
 */
object TmdbApi {
    private const val TAG = "TmdbApi"
    private const val BASE = "https://api.themoviedb.org/3"

    private var queue: RequestQueue? = null

    private fun queue(ctx: Context): RequestQueue =
        queue ?: Volley.newRequestQueue(ctx.applicationContext).also { queue = it }

    // ---------- Ready-made endpoints used by the screens ----------

    fun trending(ctx: Context, window: String, cb: (List<Movie>?, Boolean) -> Unit) =
        getList(ctx, "/trending/all/$window", "", "movie", cb)

    fun topRatedMovies(ctx: Context, cb: (List<Movie>?, Boolean) -> Unit) =
        getList(ctx, "/movie/top_rated", "", "movie", cb)

    fun topRatedSeries(ctx: Context, cb: (List<Movie>?, Boolean) -> Unit) =
        getList(ctx, "/tv/top_rated", "", "tv", cb)

    fun popularMovies(ctx: Context, cb: (List<Movie>?, Boolean) -> Unit) =
        getList(ctx, "/movie/popular", "", "movie", cb)

    fun similar(ctx: Context, movie: Movie, cb: (List<Movie>?, Boolean) -> Unit) =
        getList(ctx, "/${movie.mediaType}/${movie.id}/similar", "", movie.mediaType, cb)

    fun search(ctx: Context, query: String, cb: (List<Movie>?, Boolean) -> Unit) =
        getList(ctx, "/search/multi", "query=${Uri.encode(query)}&include_adult=false", "movie", cb)

    // ---------- Core request function ----------

    /**
     * Loads a list of movies/series.
     * cb(list, fromCache): list is null if there is neither internet nor a cached copy.
     */
    fun getList(
        ctx: Context,
        path: String,
        params: String,
        defaultType: String,
        cb: (List<Movie>?, Boolean) -> Unit
    ) {
        val app = ctx.applicationContext
        val cacheKey = "$path?$params"

        // Fallback: read the last saved response from SQLite
        fun fromCache() {
            Bg.run({ LocalDb.get(app).getCache(cacheKey) }) { json ->
                if (json == null) cb(null, true) else cb(parseList(json, defaultType), true)
            }
        }

        if (!NetworkUtil.isOnline(app)) {
            Log.d(TAG, "Offline: using cache for $cacheKey")
            fromCache()
            return
        }

        val url = "$BASE$path?api_key=${BuildConfig.TMDB_API_KEY}&language=en-US&$params"
        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                Log.d(TAG, "Loaded $path")
                Bg.exec { LocalDb.get(app).putCache(cacheKey, response) }
                cb(parseList(response, defaultType), false)
            },
            { error ->
                Log.w(TAG, "Request failed for $path: ${error.message}")
                fromCache()
            })
        queue(app).add(request)
    }

    /** Turns the TMDB JSON into Movie objects. People (media_type = "person") are skipped. */
    private fun parseList(json: String, defaultType: String): List<Movie> {
        return try {
            val results = JSONObject(json).getJSONArray("results")
            val list = mutableListOf<Movie>()
            for (i in 0 until results.length()) {
                val o = results.getJSONObject(i)
                val type = o.optString("media_type", defaultType)
                if (type != "movie" && type != "tv") continue
                val movie = Movie.fromJson(o, defaultType)
                if (movie.title.isNotEmpty()) list.add(movie)
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Could not parse TMDB response", e)
            emptyList()
        }
    }

    /** Genres + tagline for the detail screen (online only; returns null when offline). */
    fun getDetails(ctx: Context, movie: Movie, cb: (Details?) -> Unit) {
        val app = ctx.applicationContext
        if (!NetworkUtil.isOnline(app)) {
            cb(null)
            return
        }
        val url = "$BASE/${movie.mediaType}/${movie.id}?api_key=${BuildConfig.TMDB_API_KEY}&language=en-US"
        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val o = JSONObject(response)
                    val genres = o.optJSONArray("genres")
                    val names = mutableListOf<String>()
                    if (genres != null) {
                        for (i in 0 until genres.length()) {
                            names.add(genres.getJSONObject(i).optString("name"))
                        }
                    }
                    cb(Details(names.joinToString(" • "), o.optString("tagline")))
                } catch (e: Exception) {
                    Log.e(TAG, "Could not parse details", e)
                    cb(null)
                }
            },
            { error ->
                Log.w(TAG, "Details request failed: ${error.message}")
                cb(null)
            })
        queue(app).add(request)
    }
}
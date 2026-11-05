package com.example.cinepulse

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import org.json.JSONObject

/**
 * Local SQLite database. It lets the app work OFFLINE:
 *  - watchlist  : the user's watchlist (with a "pending" flag for sync)
 *  - ratings    : the user's own star ratings (with a "pending" flag)
 *  - pending_ops: comments / recommendations written while offline
 *  - api_cache  : last TMDB responses so Home/Trending still show without internet
 * SyncManager uploads everything with pending != 0 once the phone is back online.
 */
class LocalDb private constructor(ctx: Context) :
    SQLiteOpenHelper(ctx, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val TAG = "LocalDb"
        private const val DB_NAME = "cinepulse.db"
        private const val DB_VERSION = 1

        @Volatile
        private var instance: LocalDb? = null

        fun get(ctx: Context): LocalDb = instance ?: synchronized(this) {
            instance ?: LocalDb(ctx.applicationContext).also { instance = it }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE watchlist (
                uid TEXT NOT NULL,
                movie_key TEXT NOT NULL,
                movie_json TEXT NOT NULL,
                watched INTEGER NOT NULL DEFAULT 0,
                archived INTEGER NOT NULL DEFAULT 0,
                added_at INTEGER NOT NULL,
                pending INTEGER NOT NULL DEFAULT 1,
                PRIMARY KEY (uid, movie_key))"""
        )
        db.execSQL(
            """CREATE TABLE ratings (
                uid TEXT NOT NULL,
                movie_key TEXT NOT NULL,
                rating REAL NOT NULL,
                pending INTEGER NOT NULL DEFAULT 1,
                PRIMARY KEY (uid, movie_key))"""
        )
        db.execSQL(
            """CREATE TABLE pending_ops (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uid TEXT NOT NULL,
                type TEXT NOT NULL,
                movie_key TEXT NOT NULL,
                movie_json TEXT NOT NULL,
                text TEXT NOT NULL,
                created_at INTEGER NOT NULL)"""
        )
        db.execSQL(
            """CREATE TABLE api_cache (
                cache_key TEXT PRIMARY KEY,
                json TEXT NOT NULL,
                updated_at INTEGER NOT NULL)"""
        )
        Log.d(TAG, "Database created")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS watchlist")
        db.execSQL("DROP TABLE IF EXISTS ratings")
        db.execSQL("DROP TABLE IF EXISTS pending_ops")
        db.execSQL("DROP TABLE IF EXISTS api_cache")
        onCreate(db)
    }

    // =====================================================================
    // WATCHLIST
    // =====================================================================

    private fun rowToItem(c: Cursor) = WatchItem(
        movie = Movie.fromJson(JSONObject(c.getString(0))),
        watched = c.getInt(1) == 1,
        archived = c.getInt(2) == 1,
        addedAt = c.getLong(3),
        pending = c.getInt(4)
    )

    private val itemColumns = "movie_json, watched, archived, added_at, pending"

    /** Add a movie to the watchlist (flagged pending so it syncs to Firestore). */
    fun addToWatchlist(uid: String, movie: Movie) {
        val cv = ContentValues().apply {
            put("uid", uid)
            put("movie_key", movie.key)
            put("movie_json", movie.toJson().toString())
            put("watched", 0)
            put("archived", 0)
            put("added_at", System.currentTimeMillis())
            put("pending", 1)
        }
        writableDatabase.insertWithOnConflict("watchlist", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        Log.d(TAG, "Added '${movie.title}' to watchlist (pending sync)")
    }

    /** One watchlist row, or null if it is not in the list (or was removed). */
    fun getItem(uid: String, key: String): WatchItem? {
        readableDatabase.rawQuery(
            "SELECT $itemColumns FROM watchlist WHERE uid=? AND movie_key=? AND pending!=2",
            arrayOf(uid, key)
        ).use { c ->
            return if (c.moveToFirst()) rowToItem(c) else null
        }
    }

    /** Mark for deletion; SyncManager removes it from Firestore and then deletes the row. */
    fun removeFromWatchlist(uid: String, key: String) {
        val cv = ContentValues().apply { put("pending", 2) }
        writableDatabase.update("watchlist", cv, "uid=? AND movie_key=?", arrayOf(uid, key))
        Log.d(TAG, "Removed $key from watchlist (pending sync)")
    }

    fun setWatched(uid: String, key: String, watched: Boolean) {
        val cv = ContentValues().apply {
            put("watched", if (watched) 1 else 0)
            put("pending", 1)
        }
        writableDatabase.update("watchlist", cv, "uid=? AND movie_key=? AND pending!=2", arrayOf(uid, key))
        Log.d(TAG, "Set watched=$watched for $key")
    }

    /** The visible watchlist: unwatched first, newest first. Archived (completed) items are hidden. */
    fun getActiveWatchlist(uid: String): List<WatchItem> {
        val list = mutableListOf<WatchItem>()
        readableDatabase.rawQuery(
            "SELECT $itemColumns FROM watchlist WHERE uid=? AND archived=0 AND pending!=2 " +
                    "ORDER BY watched ASC, added_at DESC",
            arrayOf(uid)
        ).use { c -> while (c.moveToNext()) list.add(rowToItem(c)) }
        return list
    }

    /** Titles the user already finished (used to suggest similar movies for the next watchlist). */
    fun getArchived(uid: String, limit: Int): List<WatchItem> {
        val list = mutableListOf<WatchItem>()
        readableDatabase.rawQuery(
            "SELECT $itemColumns FROM watchlist WHERE uid=? AND archived=1 AND pending!=2 " +
                    "ORDER BY added_at DESC LIMIT $limit",
            arrayOf(uid)
        ).use { c -> while (c.moveToNext()) list.add(rowToItem(c)) }
        return list
    }

    /** Total number of titles the user has watched (including archived ones). */
    fun watchedCount(uid: String): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM watchlist WHERE uid=? AND watched=1 AND pending!=2",
            arrayOf(uid)
        ).use { c -> return if (c.moveToFirst()) c.getInt(0) else 0 }
    }

    /**
     * Reward step: moves all watched titles out of the active watchlist (archived = 1)
     * and returns them so we can find similar movies.
     */
    fun archiveWatched(uid: String): List<WatchItem> {
        val done = getActiveWatchlist(uid).filter { it.watched }
        val cv = ContentValues().apply {
            put("archived", 1)
            put("pending", 1)
        }
        writableDatabase.update(
            "watchlist", cv,
            "uid=? AND watched=1 AND archived=0 AND pending!=2", arrayOf(uid)
        )
        Log.d(TAG, "Archived ${done.size} watched titles")
        return done
    }

    /** Rows that still have to be pushed to (or deleted from) Firestore. */
    fun getPendingWatch(uid: String): List<WatchItem> {
        val list = mutableListOf<WatchItem>()
        readableDatabase.rawQuery(
            "SELECT $itemColumns FROM watchlist WHERE uid=? AND pending!=0", arrayOf(uid)
        ).use { c -> while (c.moveToNext()) list.add(rowToItem(c)) }
        return list
    }

    fun markWatchSynced(uid: String, key: String) {
        val cv = ContentValues().apply { put("pending", 0) }
        writableDatabase.update("watchlist", cv, "uid=? AND movie_key=? AND pending=1", arrayOf(uid, key))
    }

    fun deleteWatchRow(uid: String, key: String) {
        writableDatabase.delete("watchlist", "uid=? AND movie_key=?", arrayOf(uid, key))
    }

    /** Used when restoring from Firestore. A local unsynced change always wins. */
    fun upsertFromCloud(uid: String, item: WatchItem) {
        readableDatabase.rawQuery(
            "SELECT pending FROM watchlist WHERE uid=? AND movie_key=?",
            arrayOf(uid, item.movie.key)
        ).use { c ->
            if (c.moveToFirst() && c.getInt(0) != 0) return
        }
        val cv = ContentValues().apply {
            put("uid", uid)
            put("movie_key", item.movie.key)
            put("movie_json", item.movie.toJson().toString())
            put("watched", if (item.watched) 1 else 0)
            put("archived", if (item.archived) 1 else 0)
            put("added_at", item.addedAt)
            put("pending", 0)
        }
        writableDatabase.insertWithOnConflict("watchlist", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // =====================================================================
    // RATINGS (the user's own star rating)
    // =====================================================================

    fun setRating(uid: String, key: String, rating: Float, pending: Boolean = true) {
        val cv = ContentValues().apply {
            put("uid", uid)
            put("movie_key", key)
            put("rating", rating.toDouble())
            put("pending", if (pending) 1 else 0)
        }
        writableDatabase.insertWithOnConflict("ratings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        Log.d(TAG, "Saved rating $rating for $key (pending=$pending)")
    }

    fun getRating(uid: String, key: String): Float? {
        readableDatabase.rawQuery(
            "SELECT rating FROM ratings WHERE uid=? AND movie_key=?", arrayOf(uid, key)
        ).use { c -> return if (c.moveToFirst()) c.getFloat(0) else null }
    }

    fun getPendingRatings(uid: String): List<Pair<String, Float>> {
        val list = mutableListOf<Pair<String, Float>>()
        readableDatabase.rawQuery(
            "SELECT movie_key, rating FROM ratings WHERE uid=? AND pending=1", arrayOf(uid)
        ).use { c -> while (c.moveToNext()) list.add(Pair(c.getString(0), c.getFloat(1))) }
        return list
    }

    fun markRatingSynced(uid: String, key: String) {
        val cv = ContentValues().apply { put("pending", 0) }
        writableDatabase.update("ratings", cv, "uid=? AND movie_key=?", arrayOf(uid, key))
    }

    // =====================================================================
    // PENDING OPERATIONS (comments + recommendations made offline)
    // =====================================================================

    fun addPendingOp(uid: String, type: String, movie: Movie, text: String): Long {
        val cv = ContentValues().apply {
            put("uid", uid)
            put("type", type)
            put("movie_key", movie.key)
            put("movie_json", movie.toJson().toString())
            put("text", text)
            put("created_at", System.currentTimeMillis())
        }
        val id = writableDatabase.insert("pending_ops", null, cv)
        Log.d(TAG, "Queued offline $type for '${movie.title}' (id=$id)")
        return id
    }

    private fun rowToOp(c: Cursor) = PendingOp(
        id = c.getLong(0),
        uid = c.getString(1),
        type = c.getString(2),
        movie = Movie.fromJson(JSONObject(c.getString(3))),
        text = c.getString(4),
        createdAt = c.getLong(5)
    )

    fun getPendingOps(uid: String): List<PendingOp> {
        val list = mutableListOf<PendingOp>()
        readableDatabase.rawQuery(
            "SELECT id, uid, type, movie_json, text, created_at FROM pending_ops " +
                    "WHERE uid=? ORDER BY id ASC", arrayOf(uid)
        ).use { c -> while (c.moveToNext()) list.add(rowToOp(c)) }
        return list
    }

    /** Offline comments for one movie, so they can be shown with a "pending" label. */
    fun getPendingComments(uid: String, key: String): List<PendingOp> {
        val list = mutableListOf<PendingOp>()
        readableDatabase.rawQuery(
            "SELECT id, uid, type, movie_json, text, created_at FROM pending_ops " +
                    "WHERE uid=? AND type='comment' AND movie_key=? ORDER BY id DESC",
            arrayOf(uid, key)
        ).use { c -> while (c.moveToNext()) list.add(rowToOp(c)) }
        return list
    }

    fun deletePendingOp(id: Long) {
        writableDatabase.delete("pending_ops", "id=?", arrayOf(id.toString()))
    }

    /** How many changes are waiting to be synced (shown in Settings). */
    fun pendingCount(uid: String): Int {
        var total = 0
        val queries = listOf(
            "SELECT COUNT(*) FROM watchlist WHERE uid=? AND pending!=0",
            "SELECT COUNT(*) FROM ratings WHERE uid=? AND pending=1",
            "SELECT COUNT(*) FROM pending_ops WHERE uid=?"
        )
        for (q in queries) {
            readableDatabase.rawQuery(q, arrayOf(uid)).use { c ->
                if (c.moveToFirst()) total += c.getInt(0)
            }
        }
        return total
    }

    // =====================================================================
    // API CACHE (so the app still shows movies offline)
    // =====================================================================

    fun putCache(key: String, json: String) {
        val cv = ContentValues().apply {
            put("cache_key", key)
            put("json", json)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("api_cache", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getCache(key: String): String? {
        readableDatabase.rawQuery(
            "SELECT json FROM api_cache WHERE cache_key=?", arrayOf(key)
        ).use { c -> return if (c.moveToFirst()) c.getString(0) else null }
    }
}
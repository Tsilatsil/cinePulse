package com.example.cinepulse

import android.content.Context
import android.util.Log
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Result of marking a title as watched. */
data class WatchResult(
    val completed: Boolean,        // true if this finished the whole watchlist
    val added: List<Movie>,        // titles in the fresh watchlist
    val savedForLater: Boolean,    // true if we were offline: the fresh list is created after sync
    val pointsGained: Int
)

/**
 * Mini rewards system.
 *  - Points for rating, commenting, recommending and watching.
 *  - Levels based on total points.
 *  - COMPLETING THE WATCHLIST: you earn a bonus, the finished titles are archived and
 *    a brand-new watchlist is created from trending titles + titles SIMILAR to what you watched.
 */
object RewardManager {
    private const val TAG = "RewardManager"

    const val PTS_RATE = 5
    const val PTS_COMMENT = 10
    const val PTS_RECOMMEND = 15
    const val PTS_WATCHED = 10
    const val PTS_WATCHLIST_COMPLETE = 50

    /** Points needed to reach levels 0..4 */
    private val LEVEL_START = intArrayOf(0, 100, 300, 600, 1000)

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences("cinepulse_rewards", Context.MODE_PRIVATE)

    private fun pointsKey(uid: String) = "points_$uid"
    private fun pendingKey(uid: String) = "refurbish_pending_$uid"

    // ------------------------- points -------------------------

    fun points(ctx: Context): Int = prefs(ctx).getInt(pointsKey(Session.uid()), 0)

    fun setPoints(ctx: Context, uid: String, value: Int) {
        prefs(ctx).edit().putInt(pointsKey(uid), value).apply()
    }

    /** Adds points locally and mirrors the total to Firestore. Returns the new total. */
    fun addPoints(ctx: Context, amount: Int): Int {
        val uid = Session.uid()
        val total = points(ctx) + amount
        setPoints(ctx, uid, total)
        Cloud.saveProfile(uid, total)
        Log.d(TAG, "+$amount points -> $total")
        return total
    }

    // ------------------------- levels -------------------------

    /** 0 = Rookie ... 4 = Cinephile */
    fun levelIndex(points: Int): Int {
        var index = 0
        for (i in LEVEL_START.indices) {
            if (points >= LEVEL_START[i]) index = i
        }
        return index
    }

    /** Points needed for the next level, or null at the top level. */
    fun nextLevelAt(points: Int): Int? = LEVEL_START.getOrNull(levelIndex(points) + 1)

    fun levelStart(points: Int): Int = LEVEL_START[levelIndex(points)]

    fun levelNameRes(points: Int): Int = when (levelIndex(points)) {
        0 -> R.string.level_0
        1 -> R.string.level_1
        2 -> R.string.level_2
        3 -> R.string.level_3
        else -> R.string.level_4
    }

    // ------------------------- watching + completion -------------------------

    /**
     * Marks a title as watched, awards points, and if that finished the whole watchlist
     * triggers the completion reward (bonus + archive + fresh watchlist).
     */
    fun markWatched(ctx: Context, uid: String, key: String, cb: (WatchResult) -> Unit) {
        val app = ctx.applicationContext
        Bg.run({
            val db = LocalDb.get(app)
            db.setWatched(uid, key, true)
            val active = db.getActiveWatchlist(uid)
            active.isNotEmpty() && active.all { it.watched }
        }) { listComplete ->
            addPoints(app, PTS_WATCHED)
            if (!listComplete) {
                SyncManager.syncAll(app)
                cb(WatchResult(false, emptyList(), false, PTS_WATCHED))
            } else {
                Log.d(TAG, "Watchlist completed!")
                addPoints(app, PTS_WATCHLIST_COMPLETE)
                val gained = PTS_WATCHED + PTS_WATCHLIST_COMPLETE
                Bg.run({ LocalDb.get(app).archiveWatched(uid) }) {
                    if (NetworkUtil.isOnline(app)) {
                        refurbish(app, uid) { added ->
                            SyncManager.syncAll(app)
                            cb(WatchResult(true, added, false, gained))
                        }
                    } else {
                        // Offline: remember to build the fresh list as soon as we are back online
                        prefs(app).edit().putBoolean(pendingKey(uid), true).apply()
                        SyncManager.syncAll(app)
                        cb(WatchResult(true, emptyList(), true, gained))
                    }
                }
            }
        }
    }

    /**
     * Builds the "refurbished" watchlist: up to 4 titles similar to what the user just
     * finished + trending titles to fill the rest (6 in total, no duplicates).
     */
    private fun refurbish(app: Context, uid: String, cb: (List<Movie>) -> Unit) {
        Bg.run({ LocalDb.get(app).getArchived(uid, 3) }) { seeds ->
            val similarPool = mutableListOf<Movie>()
            val trendingPool = mutableListOf<Movie>()
            var remaining = seeds.size + 1   // one request per seed + one for trending

            fun oneDone() {
                remaining--
                if (remaining > 0) return
                Bg.run({
                    val db = LocalDb.get(app)
                    // Only keep titles the user has never had in their list
                    fun List<Movie>.unseen() =
                        distinctBy { it.key }.filter { db.getItem(uid, it.key) == null }

                    val similar = similarPool.unseen().take(4)
                    val trending = trendingPool.unseen()
                        .filter { t -> similar.none { it.key == t.key } }
                        .take(6 - similar.size)
                    val picks = similar + trending
                    picks.forEach { db.addToWatchlist(uid, it) }
                    Log.d(TAG, "Fresh watchlist created with ${picks.size} titles")
                    picks
                }) { picks -> cb(picks) }
            }

            TmdbApi.trending(app, "week") { list, _ ->
                trendingPool.addAll(list.orEmpty().take(15))
                oneDone()
            }
            for (seed in seeds) {
                TmdbApi.similar(app, seed.movie) { list, _ ->
                    similarPool.addAll(list.orEmpty().take(10))
                    oneDone()
                }
            }
        }
    }

    /** Called by SyncManager when the phone is back online: finishes an offline completion. */
    fun refurbishIfPending(ctx: Context) {
        val app = ctx.applicationContext
        val uid = Session.uid()
        if (uid.isEmpty() || !NetworkUtil.isOnline(app)) return
        if (!prefs(app).getBoolean(pendingKey(uid), false)) return
        prefs(app).edit().putBoolean(pendingKey(uid), false).apply()
        refurbish(app, uid) { added ->
            if (added.isNotEmpty()) {
                Notifier.show(
                    app,
                    app.getString(R.string.notif_reward_title),
                    app.getString(R.string.notif_reward_body, added.size)
                )
                SyncManager.syncAll(app)
            }
        }
    }
}

/** The "Watchlist complete!" pop-up, shared by the Watchlist and Detail screens. */
object RewardUi {
    fun show(ctx: Context, r: WatchResult) {
        val body = when {
            r.savedForLater -> ctx.getString(R.string.reward_body_offline, r.pointsGained)
            r.added.isEmpty() -> ctx.getString(R.string.reward_body_none, r.pointsGained)
            else -> ctx.getString(R.string.reward_body, r.pointsGained, r.added.size) +
                    "\n\n" + r.added.joinToString("\n") { "• ${it.title}" }
        }
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.reward_title)
            .setMessage(body)
            .setPositiveButton(R.string.reward_ok, null)
            .show()
    }
}
package com.example.cinepulse

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

/**
 * OFFLINE MODE + SYNC.
 *
 * While offline, everything is saved in SQLite with a "pending" flag
 * (watchlist changes, ratings, comments, recommendations).
 * syncAll() uploads those pending changes to Firestore once the phone is online,
 * and marks them as synced (or deletes them) in SQLite.
 *
 * It runs: when the internet comes back (CinePulseApp), when the app opens, after every
 * change the user makes, and when the user taps "Sync now" in Settings.
 */
object SyncManager {
    private const val TAG = "SyncManager"
    private const val TIMEOUT_MS = 25_000L

    private val main = Handler(Looper.getMainLooper())

    // The state below is only used on the main thread
    private var running = false
    private var rerun = false
    private var announceNext = false
    private var generation = 0
    private val waiters = mutableListOf<(Int) -> Unit>()

    /** Everything that still has to be uploaded. */
    private class Work(
        val watch: List<WatchItem>,
        val ratings: List<Pair<String, Float>>,
        val ops: List<PendingOp>
    ) {
        val total: Int get() = watch.size + ratings.size + ops.size
    }

    /**
     * Uploads pending changes. [onDone] is always called on the main thread
     * with the number of changes that were synced (0 when offline or nothing to do).
     * [announce] = show a "Back online - synced N changes" toast.
     */
    fun syncAll(ctx: Context, announce: Boolean = false, onDone: ((Int) -> Unit)? = null) {
        val app = ctx.applicationContext
        main.post {
            if (onDone != null) waiters.add(onDone)
            if (announce) announceNext = true
            if (running) {
                rerun = true          // run once more when the current sync ends
            } else {
                start(app)
            }
        }
    }

    /** After logging in: restore points + watchlist from Firestore, then upload local changes. */
    fun onLogin(ctx: Context) {
        val app = ctx.applicationContext
        val uid = Session.uid()
        if (uid.isEmpty()) return

        // Keep whichever points total is higher (local phone or cloud)
        Cloud.pullPoints(uid) { cloudPoints ->
            val local = RewardManager.points(app)
            if (cloudPoints != null) {
                val best = maxOf(local, cloudPoints)
                RewardManager.setPoints(app, uid, best)
                Cloud.saveProfile(uid, best)
            } else if (local > 0) {
                Cloud.saveProfile(uid, local)
            }
        }

        // Restore the watchlist (e.g. after reinstalling or on a new phone)
        Cloud.pullWatchlist(uid) { items ->
            if (items != null) {
                Bg.exec {
                    val db = LocalDb.get(app)
                    items.forEach { db.upsertFromCloud(uid, it) }
                    Log.d(TAG, "Restored ${items.size} watchlist items from Firestore")
                }
            }
            syncAll(app)
        }
    }

    // ------------------------------------------------------------------

    private fun start(app: Context) {
        val uid = Session.uid()
        if (uid.isEmpty() || !NetworkUtil.isOnline(app)) {
            Log.d(TAG, "Sync skipped (no user or offline)")
            finish(app, 0)
            return
        }

        running = true
        val gen = ++generation

        // Safety net: never stay "running" forever if the network drops during a sync
        main.postDelayed({
            if (running && gen == generation) {
                Log.w(TAG, "Sync timed out")
                finish(app, 0)
            }
        }, TIMEOUT_MS)

        Bg.run({
            val db = LocalDb.get(app)
            Work(db.getPendingWatch(uid), db.getPendingRatings(uid), db.getPendingOps(uid))
        }) { work -> upload(app, uid, work, gen) }
    }

    private fun upload(app: Context, uid: String, work: Work, gen: Int) {
        if (gen != generation || !running) return
        if (work.total == 0) {
            finish(app, 0)
            return
        }
        Log.d(TAG, "Uploading ${work.total} pending changes")

        var remaining = work.total
        var okCount = 0
        fun step(ok: Boolean) {
            if (ok) okCount++
            remaining--
            if (remaining == 0 && gen == generation && running) finish(app, okCount)
        }

        val db = LocalDb.get(app)

        // 1) Watchlist changes (add / watched / archive = upsert, remove = delete)
        for (item in work.watch) {
            val key = item.movie.key
            if (item.pending == 2) {
                Cloud.deleteWatchItem(uid, key) { ok ->
                    if (ok) Bg.exec { db.deleteWatchRow(uid, key) }
                    step(ok)
                }
            } else {
                Cloud.pushWatchItem(uid, item) { ok ->
                    if (ok) Bg.exec { db.markWatchSynced(uid, key) }
                    step(ok)
                }
            }
        }

        // 2) Star ratings
        for ((key, rating) in work.ratings) {
            Cloud.pushRating(uid, key, rating) { ok ->
                if (ok) Bg.exec { db.markRatingSynced(uid, key) }
                step(ok)
            }
        }

        // 3) Comments and recommendations written offline
        for (op in work.ops) {
            val done: (Boolean) -> Unit = { ok ->
                if (ok) Bg.exec { db.deletePendingOp(op.id) }
                step(ok)
            }
            if (op.type == "comment") {
                Cloud.pushComment(uid, op.movie.key, op.text, op.createdAt, done)
            } else {
                Cloud.pushRecommendation(uid, op.movie, op.text, op.createdAt, done)
            }
        }
    }

    private fun finish(app: Context, synced: Int) {
        running = false
        val callbacks = waiters.toList()
        waiters.clear()

        if (rerun) {
            // Something changed while we were syncing: go again (callbacks wait for that run)
            rerun = false
            waiters.addAll(callbacks)
            start(app)
            return
        }

        Log.d(TAG, "Sync finished, $synced changes uploaded")
        if (synced > 0 && announceNext) {
            Toast.makeText(app, app.getString(R.string.sync_done_toast, synced), Toast.LENGTH_LONG).show()
        }
        announceNext = false

        // If a watchlist was completed while offline, build the fresh list now
        RewardManager.refurbishIfPending(app)

        callbacks.forEach { it(synced) }
    }
}
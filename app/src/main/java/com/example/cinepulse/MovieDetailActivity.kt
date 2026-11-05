package com.example.cinepulse

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cinepulse.databinding.ActivityMovieDetailBinding
import com.example.cinepulse.databinding.DialogRecommendBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ListenerRegistration

/**
 * Details of one movie / series. The user can:
 *  - add / remove it from the watchlist and mark it watched (rewards!)
 *  - give a 1-5 star rating (community average is shown)
 *  - write comments (they update live for everybody)
 *  - recommend it to other CinePulse users
 *
 * Every change is saved to the local SQLite database first and then synced to Firestore,
 * so everything also works while the phone is offline.
 */
class MovieDetailActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MovieDetail"
        private const val EXTRA_MOVIE = "movie"

        fun start(ctx: Context, movie: Movie) {
            ctx.startActivity(
                Intent(ctx, MovieDetailActivity::class.java).putExtra(EXTRA_MOVIE, movie)
            )
        }
    }

    private lateinit var b: ActivityMovieDetailBinding
    private lateinit var movie: Movie
    private val uid by lazy { Session.uid() }
    private val app by lazy { applicationContext }

    private val commentAdapter = CommentAdapter()
    private var cloudComments: List<Comment> = emptyList()
    private var commentListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val passed = IntentCompat.getSerializableExtra(intent, EXTRA_MOVIE, Movie::class.java)
        if (passed == null) {
            Log.w(TAG, "No movie passed to detail screen")
            finish()
            return
        }
        movie = passed

        b = ActivityMovieDetailBinding.inflate(layoutInflater)
        setContentView(b.root)

        showHeader()

        b.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        b.btnWatchlist.setOnClickListener { toggleWatchlist() }
        b.btnWatched.setOnClickListener { markWatched() }
        b.btnRecommend.setOnClickListener { showRecommendDialog() }
        b.btnSendComment.setOnClickListener { submitComment() }
        b.ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser && rating > 0f) submitRating(rating)
        }

        b.rvComments.layoutManager = LinearLayoutManager(this)
        b.rvComments.adapter = commentAdapter

        refreshWatchButtons()
        loadMyRating()
        loadCommunityRating()
        renderComments()

        // Genres + tagline (online only)
        TmdbApi.getDetails(this, movie) { d ->
            if (d != null && !isFinishing && !isDestroyed) {
                if (d.genres.isNotEmpty()) {
                    b.tvGenres.text = d.genres
                    b.tvGenres.visibility = View.VISIBLE
                }
                if (d.tagline.isNotEmpty()) {
                    b.tvTagline.text = d.tagline
                    b.tvTagline.visibility = View.VISIBLE
                }
            }
        }

        // REAL-TIME: the comment list updates the moment anyone posts
        commentListener = Cloud.listenComments(movie.key) { list ->
            cloudComments = list
            if (!isFinishing && !isDestroyed) renderComments()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        commentListener?.remove()
        commentListener = null
    }

    // ------------------------------------------------------------------
    // Header
    // ------------------------------------------------------------------

    private fun showHeader() {
        b.tvTitle.text = movie.title
        b.tvMeta.text = metaLine(this, movie)
        b.tvOverview.text = movie.overview.ifEmpty { getString(R.string.no_overview) }
        b.tvTmdb.text = getString(R.string.tmdb_rating, movie.voteAverage)
        b.ivBackdrop.contentDescription = movie.title
        loadImage(b.ivBackdrop, movie.backdropUrl)
    }

    // ------------------------------------------------------------------
    // Watchlist buttons
    // ------------------------------------------------------------------

    private fun refreshWatchButtons() {
        Bg.run({ LocalDb.get(app).getItem(uid, movie.key) }) { item ->
            val inList = item != null && !item.archived
            val watched = item != null && (item.watched || item.archived)

            b.btnWatchlist.setText(if (inList) R.string.btn_remove_watchlist else R.string.btn_add_watchlist)
            b.btnWatchlist.setIconResource(if (inList) R.drawable.ic_delete else R.drawable.ic_add)

            b.btnWatched.visibility = if (inList || watched) View.VISIBLE else View.GONE
            b.btnWatched.isEnabled = !watched
            b.btnWatched.setText(if (watched) R.string.btn_watched else R.string.btn_mark_watched)
        }
    }

    private fun toggleWatchlist() {
        Bg.run({
            val db = LocalDb.get(app)
            val item = db.getItem(uid, movie.key)
            if (item != null && !item.archived) {
                db.removeFromWatchlist(uid, movie.key)
                false
            } else {
                db.addToWatchlist(uid, movie)
                true
            }
        }) { added ->
            Toast.makeText(
                this,
                if (added) R.string.toast_added_watchlist else R.string.toast_removed_watchlist,
                Toast.LENGTH_SHORT
            ).show()
            refreshWatchButtons()
            notifyIfOffline()
            SyncManager.syncAll(app)
        }
    }

    private fun markWatched() {
        RewardManager.markWatched(this, uid, movie.key) { result ->
            if (isFinishing || isDestroyed) return@markWatched
            refreshWatchButtons()
            if (result.completed) {
                RewardUi.show(this, result)
            } else {
                toastPoints(result.pointsGained)
            }
        }
    }

    // ------------------------------------------------------------------
    // Ratings
    // ------------------------------------------------------------------

    private fun loadMyRating() {
        Bg.run({ LocalDb.get(app).getRating(uid, movie.key) }) { rating ->
            if (rating != null) b.ratingBar.rating = rating
        }
    }

    private fun submitRating(rating: Float) {
        Bg.run({
            val db = LocalDb.get(app)
            val firstTime = db.getRating(uid, movie.key) == null
            db.setRating(uid, movie.key, rating, pending = true)
            firstTime
        }) { firstTime ->
            if (firstTime) {
                RewardManager.addPoints(app, RewardManager.PTS_RATE)
                toastPoints(RewardManager.PTS_RATE)
            } else {
                Toast.makeText(this, R.string.toast_rating_updated, Toast.LENGTH_SHORT).show()
            }
            notifyIfOffline()
            SyncManager.syncAll(app) {
                if (!isFinishing && !isDestroyed) loadCommunityRating()
            }
        }
    }

    private fun loadCommunityRating() {
        if (!NetworkUtil.isOnline(this)) {
            b.tvCommunity.setText(R.string.community_offline)
            return
        }
        Cloud.getCommunityRating(movie.key, uid) { cr ->
            if (isFinishing || isDestroyed) return@getCommunityRating
            when {
                cr == null -> b.tvCommunity.setText(R.string.community_offline)
                cr.count == 0 -> b.tvCommunity.setText(R.string.community_none)
                else -> b.tvCommunity.text = getString(R.string.community_rating, cr.average, cr.count)
            }
            // Restore my own rating if it is only stored in the cloud (e.g. after reinstalling)
            val mine = cr?.mine
            if (mine != null) {
                Bg.exec {
                    val db = LocalDb.get(app)
                    if (db.getRating(uid, movie.key) == null) db.setRating(uid, movie.key, mine, false)
                }
                if (b.ratingBar.rating == 0f) b.ratingBar.rating = mine
            }
        }
    }

    // ------------------------------------------------------------------
    // Comments
    // ------------------------------------------------------------------

    /** Shows cloud comments + comments that are still waiting to sync (labelled "pending"). */
    private fun renderComments() {
        Bg.run({ LocalDb.get(app).getPendingComments(uid, movie.key) }) { pending ->
            if (isFinishing || isDestroyed) return@run
            val postedTimes = cloudComments.map { it.createdAt }.toSet()
            val waiting = pending
                .filter { it.createdAt !in postedTimes }
                .map { Comment(Session.userName(), it.text, it.createdAt, pending = true) }
            val all = waiting + cloudComments
            commentAdapter.submit(all)
            b.tvNoComments.visibility = if (all.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun submitComment() {
        val text = b.etComment.text.toString().trim()
        if (text.isEmpty()) {
            b.tilComment.error = getString(R.string.err_comment_empty)
            return
        }
        b.tilComment.error = null
        b.etComment.text?.clear()

        Bg.run({ LocalDb.get(app).addPendingOp(uid, "comment", movie, text) }) {
            RewardManager.addPoints(app, RewardManager.PTS_COMMENT)
            toastPoints(RewardManager.PTS_COMMENT)
            renderComments()
            notifyIfOffline()
            SyncManager.syncAll(app) {
                if (!isFinishing && !isDestroyed) renderComments()
            }
        }
    }

    // ------------------------------------------------------------------
    // Recommend
    // ------------------------------------------------------------------

    private fun showRecommendDialog() {
        val dialogBinding = DialogRecommendBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.recommend_title, movie.title))
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.recommend_btn) { _, _ ->
                val note = dialogBinding.etNote.text.toString().trim()
                Bg.run({ LocalDb.get(app).addPendingOp(uid, "recommend", movie, note) }) {
                    RewardManager.addPoints(app, RewardManager.PTS_RECOMMEND)
                    Toast.makeText(
                        this,
                        getString(R.string.toast_recommended, RewardManager.PTS_RECOMMEND),
                        Toast.LENGTH_SHORT
                    ).show()
                    notifyIfOffline()
                    SyncManager.syncAll(app)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------

    private fun toastPoints(points: Int) {
        Toast.makeText(this, getString(R.string.toast_points, points), Toast.LENGTH_SHORT).show()
    }

    private fun notifyIfOffline() {
        if (!NetworkUtil.isOnline(this)) {
            Toast.makeText(this, R.string.saved_offline, Toast.LENGTH_LONG).show()
        }
    }
}
package com.example.cinepulse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cinepulse.databinding.FragmentWatchlistBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Watchlist tab. Reads from the local SQLite database (so it works offline).
 * When the last unwatched title is marked as watched, the user is rewarded and
 * a fresh watchlist is generated (see RewardManager).
 */
class WatchlistFragment : Fragment() {

    private var _b: FragmentWatchlistBinding? = null
    private val b get() = _b!!

    private lateinit var adapter: WatchlistAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentWatchlistBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = WatchlistAdapter(
            onOpen = { MovieDetailActivity.start(requireContext(), it.movie) },
            onWatched = { markWatched(it) },
            onRemove = { confirmRemove(it) }
        )
        b.rvWatchlist.layoutManager = LinearLayoutManager(requireContext())
        b.rvWatchlist.adapter = adapter

        b.swipe.setColorSchemeResources(R.color.cp_primary)
        b.swipe.setOnRefreshListener {
            // Pull to refresh = sync with the cloud, then reload the list
            SyncManager.syncAll(requireContext().applicationContext) {
                _b?.swipe?.isRefreshing = false
                load()
            }
        }

        b.btnDiscover.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_trending)
        }

        load()
    }

    override fun onResume() {
        super.onResume()
        if (_b != null) load()
    }

    /** Tabs are hidden/shown, so reload whenever this tab becomes visible. */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && _b != null) load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    private fun load() {
        val app = context?.applicationContext ?: return
        val uid = Session.uid()
        Bg.run({ LocalDb.get(app).getActiveWatchlist(uid) }) { items ->
            if (_b != null) render(items)
        }
    }

    private fun render(items: List<WatchItem>) {
        adapter.submit(items)
        val total = items.size
        val done = items.count { it.watched }
        val empty = total == 0

        b.emptyView.visibility = if (empty) View.VISIBLE else View.GONE
        b.cardReward.visibility = if (empty) View.GONE else View.VISIBLE
        b.tvProgress.text = getString(R.string.watchlist_progress, done, total)
        b.progressBar.max = if (total == 0) 1 else total
        b.progressBar.progress = done
    }

    private fun markWatched(item: WatchItem) {
        if (item.watched) return
        val ctx = context ?: return
        RewardManager.markWatched(ctx, Session.uid(), item.movie.key) { result ->
            val c = context
            if (c != null && _b != null) {
                load()
                if (result.completed) {
                    RewardUi.show(c, result)
                } else {
                    Toast.makeText(
                        c, getString(R.string.toast_points, result.pointsGained), Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun confirmRemove(item: WatchItem) {
        val ctx = context ?: return
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.btn_remove)
            .setMessage(getString(R.string.remove_confirm, item.movie.title))
            .setPositiveButton(R.string.btn_remove) { _, _ ->
                val app = ctx.applicationContext
                Bg.run({ LocalDb.get(app).removeFromWatchlist(Session.uid(), item.movie.key) }) {
                    SyncManager.syncAll(app)
                    if (_b != null) load()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
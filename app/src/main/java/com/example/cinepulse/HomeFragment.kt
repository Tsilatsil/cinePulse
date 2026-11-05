package com.example.cinepulse

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cinepulse.databinding.FragmentHomeBinding
import com.example.cinepulse.databinding.ItemHomeRowBinding

/**
 * Home tab. Shows several lists at once:
 *  Trending Today (big cards), Top Rated Movies, Top Rated Series, Popular Right Now,
 *  Community Picks (recommended by other users) and "Because you watched ..." (similar titles).
 */
class HomeFragment : Fragment() {

    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!

    /** One horizontal row (title + list of posters) */
    private class Row(val root: View, val title: TextView, val adapter: PosterAdapter)

    private val heroAdapter = HeroAdapter { openDetail(it) }
    private lateinit var rowTopMovies: Row
    private lateinit var rowTopSeries: Row
    private lateinit var rowPopular: Row
    private lateinit var rowCommunity: Row
    private lateinit var rowBecause: Row

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentHomeBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.rvHero.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        b.rvHero.adapter = heroAdapter

        // Rows are added in the order they appear on screen
        rowBecause = addRow(getString(R.string.home_because, ""), hidden = true)
        rowTopMovies = addRow(getString(R.string.home_top_movies), hidden = false)
        rowTopSeries = addRow(getString(R.string.home_top_series), hidden = false)
        rowPopular = addRow(getString(R.string.home_popular), hidden = false)
        rowCommunity = addRow(getString(R.string.home_community), hidden = true)

        b.cardSearch.setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }
        b.tvSeeAllTrending.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_trending)
        }
        b.swipe.setColorSchemeResources(R.color.cp_primary)
        b.swipe.setOnRefreshListener { loadAll() }

        refreshHeader()
        loadAll()
    }

    override fun onResume() {
        super.onResume()
        if (_b != null) refreshHeader()
    }

    /** Tabs are shown/hidden (not recreated), so refresh when Home becomes visible again. */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && _b != null) {
            refreshHeader()
            loadBecause { }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    // ------------------------------------------------------------------

    private fun refreshHeader() {
        b.tvGreeting.text = getString(R.string.home_greeting, Session.userName())
        b.tvPoints.text = getString(R.string.points_chip, RewardManager.points(requireContext()))
    }

    private fun addRow(title: String, hidden: Boolean): Row {
        val rb = ItemHomeRowBinding.inflate(layoutInflater, b.rowsContainer, false)
        rb.rowTitle.text = title
        val adapter = PosterAdapter { openDetail(it) }
        rb.rowList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rb.rowList.adapter = adapter
        if (hidden) rb.root.visibility = View.GONE
        b.rowsContainer.addView(rb.root)
        return Row(rb.root, rb.rowTitle, adapter)
    }

    private fun fillRow(row: Row, list: List<Movie>?) {
        if (list.isNullOrEmpty()) {
            row.root.visibility = View.GONE
        } else {
            row.adapter.submit(list.take(15))
            row.root.visibility = View.VISIBLE
        }
    }

    private fun loadAll() {
        val ctx = requireContext().applicationContext
        var remaining = 6
        fun finished() {
            remaining--
            if (remaining <= 0) _b?.swipe?.isRefreshing = false
        }
        b.swipe.isRefreshing = true

        TmdbApi.trending(ctx, "day") { list, _ ->
            if (_b != null) heroAdapter.submit(list.orEmpty().take(8))
            finished()
        }
        TmdbApi.topRatedMovies(ctx) { list, _ ->
            if (_b != null) fillRow(rowTopMovies, list)
            finished()
        }
        TmdbApi.topRatedSeries(ctx) { list, _ ->
            if (_b != null) fillRow(rowTopSeries, list)
            finished()
        }
        TmdbApi.popularMovies(ctx) { list, _ ->
            if (_b != null) fillRow(rowPopular, list)
            finished()
        }
        // Recommendations written by other CinePulse users (Firestore)
        Cloud.getRecommendations(10) { recs ->
            if (_b != null) {
                if (recs.isNullOrEmpty()) {
                    fillRow(rowCommunity, null)
                } else {
                    rowCommunity.adapter.subtitles =
                        recs.associate { it.movie.key to getString(R.string.by_user, it.userName) }
                    fillRow(rowCommunity, recs.map { it.movie }.distinctBy { it.key })
                }
            }
            finished()
        }
        loadBecause { finished() }
    }

    /** "Because you watched X": similar titles to the last completed watchlist item. */
    private fun loadBecause(onDone: () -> Unit) {
        val ctx = requireContext().applicationContext
        val uid = Session.uid()
        Bg.run({ LocalDb.get(ctx).getArchived(uid, 1).firstOrNull() }) { item ->
            if (_b == null) {
                onDone()
            } else if (item == null) {
                fillRow(rowBecause, null)
                onDone()
            } else {
                rowBecause.title.text = getString(R.string.home_because, item.movie.title)
                TmdbApi.similar(ctx, item.movie) { list, _ ->
                    if (_b != null) fillRow(rowBecause, list)
                    onDone()
                }
            }
        }
    }

    private fun openDetail(movie: Movie) {
        MovieDetailActivity.start(requireContext(), movie)
    }
}
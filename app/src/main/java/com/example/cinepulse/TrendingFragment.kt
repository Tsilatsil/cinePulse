package com.example.cinepulse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cinepulse.databinding.FragmentTrendingBinding

/**
 * Trending Now tab: ranked grid of what everyone is watching.
 * Chips let the user switch between Today / This week and All / Movies / Series.
 */
class TrendingFragment : Fragment() {

    private var _b: FragmentTrendingBinding? = null
    private val b get() = _b!!

    private val adapter = GridAdapter(showRank = true) { openDetail(it) }

    private var window = "day"        // "day" or "week"
    private var typeFilter = "all"    // "all", "movie" or "tv"
    private var allItems: List<Movie> = emptyList()
    private var fromCache = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentTrendingBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.rvTrending.layoutManager = GridLayoutManager(requireContext(), 2)
        b.rvTrending.adapter = adapter
        b.swipe.setColorSchemeResources(R.color.cp_primary)
        b.swipe.setOnRefreshListener { load() }

        b.chipGroupPeriod.setOnCheckedStateChangeListener { _, ids ->
            window = if (ids.firstOrNull() == R.id.chipWeek) "week" else "day"
            load()
        }
        b.chipGroupType.setOnCheckedStateChangeListener { _, ids ->
            typeFilter = when (ids.firstOrNull()) {
                R.id.chipMovies -> "movie"
                R.id.chipSeries -> "tv"
                else -> "all"
            }
            applyFilter()
        }

        load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    private fun load() {
        b.swipe.isRefreshing = true
        TmdbApi.trending(requireContext().applicationContext, window) { list, cached ->
            if (_b != null) {
                b.swipe.isRefreshing = false
                allItems = list.orEmpty()
                fromCache = cached
                applyFilter()
            }
        }
    }

    /** Filters the loaded list locally (no new network call needed). */
    private fun applyFilter() {
        val shown = if (typeFilter == "all") allItems else allItems.filter { it.mediaType == typeFilter }
        adapter.submit(shown)
        b.tvEmpty.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
        b.tvInfo.visibility = if (fromCache && shown.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun openDetail(movie: Movie) {
        MovieDetailActivity.start(requireContext(), movie)
    }
}
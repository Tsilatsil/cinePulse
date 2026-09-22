package com.cinepulse.app.ui.episodes

import androidx.lifecycle.ViewModel
import com.cinepulse.app.data.remote.tmdb.TmdbApi
import com.cinepulse.app.data.repository.UserMediaRepository
import android.content.Context

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.cinepulse.app.data.remote.tmdb.TmdbMedia

data class EpisodeUiState(
    val show: TmdbMedia? = null,
    val seasons: List<TmdbSeason> = emptyList(),
    val episodes: List<TmdbEpisode> = emptyList(),
    val watched: Set<String> = emptySet(),
    val selectedSeason: Int = 1,
    val completionPct: Float = 0f
)

data class TmdbSeason(val seasonNumber: Int)
data class TmdbEpisode(val episodeNumber: Int, val name: String, val airDate: String? = null)

class EpisodeViewModel(
    private val tmdb: TmdbApi,
    private val repo: UserMediaRepository,
    private val context: Context,
    private val tmdbId: Int
) : ViewModel() {
    private val _state = MutableStateFlow(EpisodeUiState())
    val state: StateFlow<EpisodeUiState> = _state.asStateFlow()

    fun loadSeason(n: Int) {}
    fun toggle(s: Int, e: Int) {}
}

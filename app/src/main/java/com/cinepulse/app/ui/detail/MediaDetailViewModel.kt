// ui/detail/MediaDetailViewModel.kt
package com.cinepulse.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinepulse.app.data.local.entity.*
import com.cinepulse.app.data.remote.tmdb.*
import com.cinepulse.app.data.repository.UserMediaRepository
import com.cinepulse.app.sync.SyncWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class StreamingAvailability {
    data class Available(val providers: List<TmdbProvider>, val link: String?) : StreamingAvailability()
    data class NotInRegion(val region: String) : StreamingAvailability()
    object Unknown : StreamingAvailability()
}

data class MediaDetailUiState(
    val detail: TmdbMedia? = null,
    val userStatus: WatchStatus? = null,
    val userRating: Int? = null,
    val streaming: StreamingAvailability = StreamingAvailability.Unknown,
    val loading: Boolean = true
)

class DetailViewModel(
    private val tmdb: TmdbApi,
    private val repo: UserMediaRepository,
    private val appContext: android.content.Context,
    private val tmdbId: Int,
    private val mediaType: MediaType,
    private val region: String = "US"
) : ViewModel() {

    private val _state = MutableStateFlow(MediaDetailUiState())
    val state: StateFlow<MediaDetailUiState> = _state.asStateFlow()

    init {
        loadDetail()
        observeUserState()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            val detail = runCatching {
                if (mediaType == MediaType.MOVIE) tmdb.movieDetail(tmdbId)
                else tmdb.tvDetail(tmdbId)
            }.getOrNull()

            val streaming = runCatching {
                val resp = if (mediaType == MediaType.MOVIE) tmdb.movieProviders(tmdbId)
                else tmdb.tvProviders(tmdbId)
                val regionData = resp.results[region]
                when {
                    regionData == null -> StreamingAvailability.NotInRegion(region)
                    regionData.flatrate.isNullOrEmpty() &&
                            regionData.rent.isNullOrEmpty() &&
                            regionData.buy.isNullOrEmpty() -> StreamingAvailability.NotInRegion(region)
                    else -> StreamingAvailability.Available(
                        providers = regionData.flatrate ?: emptyList(),
                        link = regionData.link
                    )
                }
            }.getOrElse { StreamingAvailability.Unknown }

            _state.update {
                it.copy(detail = detail, streaming = streaming, loading = false)
            }
        }
    }

    private fun observeUserState() {
        viewModelScope.launch {
            // simple poll — for production use a Flow from a query
            val existing = repo.observeWatchlist()
                .map { list -> list.firstOrNull { it.tmdbId == tmdbId && it.mediaType == mediaType } }
                .collect { item ->
                    _state.update {
                        it.copy(userStatus = item?.status, userRating = item?.rating)
                    }
                }
        }
    }

    fun setStatus(status: WatchStatus) {
        val d = _state.value.detail ?: return
        viewModelScope.launch {
            repo.setStatus(
                tmdbId = tmdbId,
                mediaType = mediaType,
                title = d.title ?: d.name ?: "",
                posterPath = d.posterPath,
                status = status
            )
            SyncWorker.enqueue(appContext)
        }
    }

    fun setRating(rating: Int) {
        viewModelScope.launch {
            repo.setRating(tmdbId, mediaType, rating)
            SyncWorker.enqueue(appContext)
        }
    }
}

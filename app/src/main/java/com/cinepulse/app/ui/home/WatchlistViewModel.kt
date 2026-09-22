package com.cinepulse.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinepulse.app.data.local.entity.MediaType
import com.cinepulse.app.data.local.entity.UserMediaEntity
import com.cinepulse.app.data.repository.UserMediaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WatchlistUiState(
    val items: List<UserMediaEntity> = emptyList()
)

class WatchlistViewModel(private val repo: UserMediaRepository) : ViewModel() {

    val state: StateFlow<WatchlistUiState> = repo.observeWatchlist()
        .map { WatchlistUiState(items = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WatchlistUiState()
        )

    fun updateComment(tmdbId: Int, mediaType: MediaType, comment: String) {
        viewModelScope.launch {
            repo.updateComment(tmdbId, mediaType, comment)
        }
    }

    fun removeItem(tmdbId: Int, mediaType: MediaType) {
        viewModelScope.launch {
            repo.deleteFromWatchlist(tmdbId, mediaType)
        }
    }
}

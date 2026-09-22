package com.cinepulse.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinepulse.app.data.remote.tmdb.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = false,
    val trending: List<TmdbMedia> = emptyList(),
    val query: String = "",
    val searchResults: List<TmdbMedia> = emptyList(),
    val error: String? = null
)

class HomeViewModel(private val api: TmdbApi) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { api.trending().results }
                .onSuccess { list ->
                    _state.update { it.copy(loading = false, trending = list) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message) }
                }
        }
    }

    fun onQuery(q: String) {
        _state.update { it.copy(query = q) }
        if (q.length >= 2) {
            viewModelScope.launch {
                runCatching { api.searchMulti(q).results }
                    .onSuccess { list ->
                        _state.update { it.copy(searchResults = list) }
                    }
            }
        }
    }
}

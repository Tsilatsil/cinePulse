package com.example.cinepulse

import com.cinepulse.app.data.remote.tmdb.TmdbApi
import com.cinepulse.app.data.remote.tmdb.TmdbMedia
import com.cinepulse.app.data.remote.tmdb.TmdbSearchResponse
import com.cinepulse.app.data.remote.tmdb.TmdbWatchProvidersResponse
import com.cinepulse.app.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Detailed Unit Testing for HomeViewModel as required by the Assessment Rubric.
 * Reference: Learning Unit 2 - Conducting detailed unit testing to verify UI state management.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Fake implementation of the TMDB REST API to allow offline testing
    private class FakeTmdbApi : TmdbApi {
        var shouldFail = false
        var trendingList = listOf(
            TmdbMedia(id = 1, title = "Inception", mediaType = "movie"),
            TmdbMedia(id = 2, name = "Breaking Bad", mediaType = "tv")
        )
        var searchList = listOf(
            TmdbMedia(id = 1, title = "Inception", mediaType = "movie")
        )

        override suspend fun searchMulti(query: String, page: Int): TmdbSearchResponse {
            if (shouldFail) throw RuntimeException("Network Error")
            return TmdbSearchResponse(page = 1, results = searchList, totalPages = 1)
        }

        override suspend fun trending(page: Int): TmdbSearchResponse {
            if (shouldFail) throw RuntimeException("Trending Network Error")
            return TmdbSearchResponse(page = 1, results = trendingList, totalPages = 1)
        }

        override suspend fun movieDetail(id: Int): TmdbMedia = TODO()
        override suspend fun tvDetail(id: Int): TmdbMedia = TODO()
        override suspend fun movieProviders(id: Int): TmdbWatchProvidersResponse = TODO()
        override suspend fun tvProviders(id: Int): TmdbWatchProvidersResponse = TODO()
    }

    private lateinit var fakeApi: FakeTmdbApi
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        println("[TEST LOG] Setting up Main dispatcher override for coroutines test execution.")
        Dispatchers.setMain(testDispatcher)
        fakeApi = FakeTmdbApi()
    }

    @After
    fun tearDown() {
        println("[TEST LOG] Tearing down Main dispatcher override.")
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoad_Success() = runTest(testDispatcher) {
        println("[TEST LOG] Starting testInitialLoad_Success")
        viewModel = HomeViewModel(fakeApi)
        
        // Advance dispatcher to execute the init block launch coroutine
        advanceUntilIdle()

        val state = viewModel.state.value
        println("[TEST LOG] State after load: trendingCount=${state.trending.size}, error=${state.error}")

        assertEquals(false, state.loading)
        assertEquals(2, state.trending.size)
        assertEquals("Inception", state.trending[0].title)
        assertEquals("Breaking Bad", state.trending[1].name)
    }

    @Test
    fun testInitialLoad_Failure() = runTest(testDispatcher) {
        println("[TEST LOG] Starting testInitialLoad_Failure")
        fakeApi.shouldFail = true
        viewModel = HomeViewModel(fakeApi)

        advanceUntilIdle()

        val state = viewModel.state.value
        println("[TEST LOG] State after failure: trendingCount=${state.trending.size}, error=${state.error}")

        assertEquals(false, state.loading)
        assertTrue(state.trending.isEmpty())
        assertNotNull(state.error)
        assertEquals("Trending Network Error", state.error)
    }

    @Test
    fun testSearchQuery_Valid() = runTest(testDispatcher) {
        println("[TEST LOG] Starting testSearchQuery_Valid")
        viewModel = HomeViewModel(fakeApi)
        advanceUntilIdle()

        viewModel.onQuery("Incept")
        advanceUntilIdle()

        val state = viewModel.state.value
        println("[TEST LOG] State after valid search: searchResultsCount=${state.searchResults.size}")

        assertEquals("Incept", state.query)
        assertEquals(1, state.searchResults.size)
        assertEquals("Inception", state.searchResults[0].title)
    }
}

// ui/CinePulseNavHost.kt
package com.cinepulse.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.cinepulse.app.CinePulseApp
import com.cinepulse.app.data.local.entity.MediaType
import com.cinepulse.app.data.remote.tmdb.TmdbApi
import com.cinepulse.app.ui.detail.DetailScreen
import com.cinepulse.app.ui.detail.DetailViewModel
import com.cinepulse.app.ui.home.HomeScreen
import com.cinepulse.app.ui.home.HomeViewModel

@Composable
fun CinePulseNavHost(app: CinePulseApp) {
    val nav = rememberNavController()
    val tmdbApi: TmdbApi = app.container.tmdb

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            val vm: HomeViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(tmdbApi) as T
            })
            HomeScreen(vm) { id, type ->
                nav.navigate("detail/$id/$type")
            }
        }
        composable(
            route = "detail/{id}/{type}",
            arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("type") { type = NavType.StringType }
            )
        ) { entry ->
            val id = entry.arguments!!.getInt("id")
            val typeStr = entry.arguments!!.getString("type")!!.uppercase()
            val type = if (typeStr == "TV") MediaType.TV else MediaType.MOVIE
            val vm: DetailViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DetailViewModel(
                        tmdb = tmdbApi,
                        repo = app.container.repo,
                        appContext = app,
                        tmdbId = id,
                        mediaType = type
                    ) as T
            })
            DetailScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onOpenEpisodes = { }
            )
        }
    }
}
package com.cinepulse.app.ui


import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.cinepulse.app.CinePulseApp
import com.cinepulse.app.data.local.entity.MediaType
import com.cinepulse.app.ui.auth.AuthScreen
import com.cinepulse.app.ui.auth.AuthViewModel
import com.cinepulse.app.ui.detail.DetailScreen
import com.cinepulse.app.ui.detail.DetailViewModel
import com.cinepulse.app.ui.episodes.EpisodeScreen
import com.cinepulse.app.ui.episodes.EpisodeViewModel
import com.cinepulse.app.ui.home.HomeScreen
import com.cinepulse.app.ui.home.HomeViewModel
import com.cinepulse.app.ui.home.SearchScreen
import com.cinepulse.app.ui.home.WatchlistScreen
import com.cinepulse.app.ui.home.WatchlistViewModel
import com.cinepulse.app.ui.profile.ProfileScreen
import com.cinepulse.app.ui.profile.ProfileViewModel
import com.cinepulse.app.ui.profile.SettingsScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)
private val tabs = listOf(
    Tab("home", "Home", Icons.Default.Home),
    Tab("watchlist", "Watchlist", Icons.AutoMirrored.Filled.List),
    Tab("search", "Search", Icons.Default.Search),
    Tab("profile", "Profile", Icons.Default.Person)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(app: CinePulseApp) {
    val nav = rememberNavController()
    val authed = remember { app.container.authManager.isLoggedIn() }
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showBottomBar = currentRoute in tabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = currentRoute == t.route,
                            onClick = {
                                if (currentRoute != t.route) {
                                    nav.navigate(t.route) {
                                        popUpTo(nav.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(t.icon, t.label) },
                            label = { Text(t.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = if (authed) "home" else "auth",
            modifier = Modifier.padding(padding)
        ) {
            composable("auth") {
                val vm: AuthViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        AuthViewModel(app.container.authManager) as T
                })
                AuthScreen(vm) {
                    nav.navigate("home") { popUpTo("auth") { inclusive = true } }
                }
            }

            composable("home") {
                HomeTab(app, nav)
            }

            composable("watchlist") {
                WatchlistTab(app, nav)
            }

            composable("search") {
                SearchTab(app, nav)
            }

            composable("profile") {
                ProfileTab(app, onNavigateToSettings = { nav.navigate("settings") }) {
                    nav.navigate("auth") { popUpTo("home") { inclusive = true } }
                }
            }

            composable("settings") {
                SettingsScreen(
                    settingsRepo = app.container.settingsRepo,
                    onBack = { nav.popBackStack() }
                )
            }

            composable(
                "detail/{id}/{type}",
                arguments = listOf(
                    navArgument("id") { type = NavType.IntType },
                    navArgument("type") { type = NavType.StringType }
                )
            ) { entry ->
                val id = entry.arguments!!.getInt("id")
                val typeStr = entry.arguments!!.getString("type")!!.lowercase()
                val type = if (typeStr == "tv") MediaType.TV else MediaType.MOVIE
                val vm: DetailViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        DetailViewModel(app.container.tmdb, app.container.repo, app, id, type) as T
                })
                DetailScreen(
                    vm,
                    onBack = { nav.popBackStack() },
                    onOpenEpisodes = { nav.navigate("episodes/$id") }
                )
            }

            composable(
                "episodes/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { entry ->
                val id = entry.arguments!!.getInt("id")
                val vm: EpisodeViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        EpisodeViewModel(app.container.tmdb, app.container.repo, app, id) as T
                })
                EpisodeScreen(vm, onBack = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun WatchlistTab(app: CinePulseApp, nav: NavController) {
    val vm: WatchlistViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WatchlistViewModel(app.container.repo) as T
    })
    WatchlistScreen(vm, onOpenDetail = { id, type -> nav.navigate("detail/$id/$type") })
}

@Composable
private fun HomeTab(app: CinePulseApp, nav: NavController) {
    val vm: HomeViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(app.container.tmdb) as T
    })
    HomeScreen(vm, onMediaClick = { id, type -> nav.navigate("detail/$id/$type") })
}

@Composable
private fun SearchTab(app: CinePulseApp, nav: NavController) {
    val vm: HomeViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(app.container.tmdb) as T
    })
    SearchScreen(vm, onOpen = { id, type -> nav.navigate("detail/$id/$type") })
}

@Composable
private fun ProfileTab(app: CinePulseApp, onNavigateToSettings: () -> Unit, onSignedOut: () -> Unit) {
    val vm: ProfileViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProfileViewModel(app.container.repo, app.container.authManager) as T
    })
    ProfileScreen(vm, onNavigateToSettings = onNavigateToSettings, onSignOut = { vm.signOut(); onSignedOut() })
}

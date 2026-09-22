// ui/home/HomeScreen.kt
package com.cinepulse.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import com.cinepulse.app.ui.components.CinePulseLogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onMediaClick: (Int, String) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CinePulseLogo(Modifier.size(32.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("CinePulse")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            state.error != null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center
            ) {
                Text("Error: ${state.error}")
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        Text(
                            "Trending Today", style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(state.trending) { media ->
                        MediaRow(media) {
                            val type = media.mediaType ?: if (media.title != null) "movie" else "tv"
                            onMediaClick(media.id, type)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaRow(
    media: com.cinepulse.app.data.remote.tmdb.TmdbMedia,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "https://image.tmdb.org/t/p/w200${media.posterPath}",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 60.dp, height = 90.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(media.title ?: media.name ?: "", style = MaterialTheme.typography.bodyLarge)
            Text("★ ${media.voteAverage ?: 0.0}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
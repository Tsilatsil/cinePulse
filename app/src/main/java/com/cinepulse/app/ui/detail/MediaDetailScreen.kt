// ui/detail/MediaDetailScreen.kt
package com.cinepulse.app.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cinepulse.app.data.local.entity.WatchStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    vm: DetailViewModel,
    onBack: () -> Unit,
    onOpenEpisodes: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val detail = state.detail ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail.title ?: detail.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w780${detail.backdropPath ?: detail.posterPath}",
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Column(Modifier.padding(16.dp)) {
                Text(detail.title ?: detail.name ?: "", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(detail.overview ?: "", style = MaterialTheme.typography.bodyMedium)
                
                if (detail.isTv) {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onOpenEpisodes) {
                        Text("View Episodes")
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Status", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.padding(vertical = 8.dp)) {
                    WatchStatus.entries.forEach { s ->
                        FilterChip(
                            selected = state.userStatus == s,
                            onClick = { vm.setStatus(s) },
                            label = { Text(s.name.replace('_', ' ')) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Text("Your rating", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.padding(vertical = 8.dp)) {
                    (1..5).forEach { star ->
                        TextButton(onClick = { vm.setRating(star) }) {
                            Text(if ((state.userRating ?: 0) >= star) "★" else "☆")
                        }
                    }
                }

                when (val s = state.streaming) {
                    is StreamingAvailability.Available -> {
                        Text("Streaming on:", style = MaterialTheme.typography.titleMedium)
                        s.providers.forEach { p ->
                            Text("• ${p.providerName}")
                        }
                    }
                    is StreamingAvailability.NotInRegion ->
                        Text("Not streaming in ${s.region}", style = MaterialTheme.typography.bodyMedium)
                    StreamingAvailability.Unknown ->
                        Text("Availability data unavailable", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

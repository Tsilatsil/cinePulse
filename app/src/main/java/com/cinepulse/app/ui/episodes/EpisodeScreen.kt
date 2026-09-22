package com.cinepulse.app.ui.episodes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeScreen(vm: EpisodeViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.show?.name ?: "Episodes") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LinearProgressIndicator(
                progress = { s.completionPct },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            Text(
                "${(s.completionPct * 100).toInt()}% complete",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                items(s.seasons) { season ->
                    FilterChip(
                        selected = s.selectedSeason == season.seasonNumber,
                        onClick = { vm.loadSeason(season.seasonNumber) },
                        label = { Text("S${season.seasonNumber}") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            LazyColumn(Modifier.fillMaxSize()) {
                items(s.episodes) { ep ->
                    val watched = s.watched.contains("${s.selectedSeason}:${ep.episodeNumber}")
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.toggle(s.selectedSeason, ep.episodeNumber) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (watched) Icons.Default.CheckCircle
                            else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (watched) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "E${ep.episodeNumber} • ${ep.name}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            ep.airDate?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}


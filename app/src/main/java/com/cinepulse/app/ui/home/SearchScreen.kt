package com.cinepulse.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@Composable
fun SearchScreen(vm: HomeViewModel, onOpen: (Int, String) -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = s.query,
            onValueChange = vm::onQuery,
            placeholder = { Text("Search movies and TV shows…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )

        if (s.query.length < 2) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Type at least 2 characters", style = MaterialTheme.typography.bodyMedium)
            }
            return
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(s.searchResults) { m ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(m.id, if (m.isTv) "tv" else "movie") }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w154${m.posterPath}",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(60.dp, 90.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(m.displayTitle, style = MaterialTheme.typography.bodyLarge)
                        Text(if (m.isTv) "TV" else "Movie",
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}


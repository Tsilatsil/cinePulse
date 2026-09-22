package com.cinepulse.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProfileScreen(vm: ProfileViewModel, onNavigateToSettings: () -> Unit, onSignOut: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(s.displayName, style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Text(s.email, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat("Logged", s.totalLogged)
            Stat("Completed", s.completed)
            Stat("Watching", s.watching)
            Stat("Streak", s.streakDays)
        }

        Spacer(Modifier.height(24.dp))
        Text("Achievements", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        if (s.achievements.isEmpty()) {
            Text("No badges yet. Log a title to unlock your first!",
                style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(s.achievements) { a ->
                    Card {
                        Column(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🏆", style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(a.title, style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text(a.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onNavigateToSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Settings")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text("Sign out")
        }
    }
}

@Composable
private fun Stat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}


// data/local/entity/UserMediaEntity.kt
package com.cinepulse.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MediaType { MOVIE, TV }
enum class WatchStatus { PLAN_TO_WATCH, WATCHING, COMPLETED, DROPPED }
enum class SyncState { SYNCED, PENDING_UPLOAD, CONFLICT }

@Entity(
    tableName = "user_media",
    primaryKeys = ["tmdbId", "mediaType"],
    indices = [Index("syncState"), Index("updatedAt")]
)
data class UserMediaEntity(
    val tmdbId: Int,
    val mediaType: MediaType,
    val title: String,
    val posterPath: String?,
    val status: WatchStatus,
    val rating: Int?,              // 1..5, null = unrated
    val review: String?,
    val updatedAt: Long,           // device epoch millis at user action
    val syncState: SyncState = SyncState.PENDING_UPLOAD
)

@Entity(
    tableName = "episodes_watched",
    primaryKeys = ["tmdbId", "season", "episode"],
    indices = [Index("syncState"), Index("watchedAt")]
)
data class EpisodeWatchedEntity(
    val tmdbId: Int,
    val season: Int,
    val episode: Int,
    val watchedAt: Long,           // device epoch millis
    val syncState: SyncState = SyncState.PENDING_UPLOAD
)

@Entity(tableName = "pending_ops")
data class PendingOpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val opType: String,            // "UPSERT_USER_MEDIA" | "DELETE_USER_MEDIA" | "UPSERT_EPISODE"
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
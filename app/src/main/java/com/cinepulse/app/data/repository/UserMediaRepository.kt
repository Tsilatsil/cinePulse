// data/repository/UserMediaRepository.kt
package com.cinepulse.app.data.repository

import com.cinepulse.app.data.local.CinePulseDatabase
import com.cinepulse.app.data.local.entity.*
import com.cinepulse.app.data.remote.tmdb.firestore.FirestoreUserDataSource
import kotlinx.coroutines.flow.Flow

class UserMediaRepository(
    private val db: CinePulseDatabase,
    private val firestore: FirestoreUserDataSource
) {
    fun observeWatchlist(): Flow<List<UserMediaEntity>> = db.userMediaDao().observeAll()

    fun observeEpisodes(showId: Int): Flow<List<EpisodeWatchedEntity>> =
        db.episodeDao().observeForShow(showId)

    /**
     * Local-first write. UI sees the change immediately; sync happens later.
     */
    suspend fun setStatus(
        tmdbId: Int,
        mediaType: MediaType,
        title: String,
        posterPath: String?,
        status: WatchStatus
    ) {
        val existing = db.userMediaDao().getById(tmdbId, mediaType)
        val now = System.currentTimeMillis()   // device timestamp at action time
        val updated = (existing ?: UserMediaEntity(
            tmdbId = tmdbId,
            mediaType = mediaType,
            title = title,
            posterPath = posterPath,
            status = status,
            rating = null,
            review = null,
            updatedAt = now
        )).copy(status = status, updatedAt = now, syncState = SyncState.PENDING_UPLOAD)

        db.userMediaDao().upsert(updated)
        // enqueue for background sync
        db.pendingOpsDao().enqueue(
            PendingOpEntity(
                opType = "UPSERT_USER_MEDIA",
                payloadJson = """{"tmdbId":$tmdbId,"mediaType":"${mediaType.name}"}"""
            )
        )
    }

    suspend fun setRating(tmdbId: Int, mediaType: MediaType, rating: Int) {
        val existing = db.userMediaDao().getById(tmdbId, mediaType) ?: return
        db.userMediaDao().upsert(
            existing.copy(
                rating = rating.coerceIn(1, 5),
                updatedAt = System.currentTimeMillis(),
                syncState = SyncState.PENDING_UPLOAD
            )
        )
    }

    suspend fun updateComment(tmdbId: Int, mediaType: MediaType, comment: String) {
        val existing = db.userMediaDao().getById(tmdbId, mediaType) ?: return
        db.userMediaDao().upsert(
            existing.copy(
                review = comment,
                updatedAt = System.currentTimeMillis(),
                syncState = SyncState.PENDING_UPLOAD
            )
        )
    }

    suspend fun deleteFromWatchlist(tmdbId: Int, mediaType: MediaType) {
        db.userMediaDao().deleteById(tmdbId, mediaType)
    }

    suspend fun markEpisodeWatched(showId: Int, season: Int, episode: Int) {
        val ep = EpisodeWatchedEntity(
            tmdbId = showId,
            season = season,
            episode = episode,
            watchedAt = System.currentTimeMillis(),   // device time, per streak rule
            syncState = SyncState.PENDING_UPLOAD
        )
        db.episodeDao().upsert(ep)
    }

    // Called by the sync worker
    suspend fun pushPending() {
        db.userMediaDao().pendingUploads().forEach { item ->
            runCatching {
                firestore.upsertUserMedia(item)
                db.userMediaDao().markSynced(item.tmdbId, item.mediaType)
            }
        }
        db.episodeDao().pendingUploads().forEach { ep ->
            runCatching {
                firestore.upsertEpisode(ep)
                db.episodeDao().markSynced(ep.tmdbId, ep.season, ep.episode)
            }
        }
    }
}
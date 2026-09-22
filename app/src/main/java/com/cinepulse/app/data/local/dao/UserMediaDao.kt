// data/local/dao/UserMediaDao.kt
package com.cinepulse.app.data.local.dao

import androidx.room.*
import com.cinepulse.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMediaDao {

    @Query("SELECT * FROM user_media ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<UserMediaEntity>>

    @Query("SELECT * FROM user_media WHERE tmdbId = :tmdbId AND mediaType = :type")
    suspend fun getById(tmdbId: Int, type: MediaType): UserMediaEntity?

    @Query("SELECT * FROM user_media WHERE syncState = 'PENDING_UPLOAD'")
    suspend fun pendingUploads(): List<UserMediaEntity>

    @Upsert
    suspend fun upsert(item: UserMediaEntity)

    @Query("UPDATE user_media SET syncState = 'SYNCED' WHERE tmdbId = :tmdbId AND mediaType = :type")
    suspend fun markSynced(tmdbId: Int, type: MediaType)

    @Query("SELECT COUNT(*) FROM user_media WHERE status = 'COMPLETED'")
    suspend fun completedCount(): Int

    @Query("DELETE FROM user_media WHERE tmdbId = :tmdbId AND mediaType = :type")
    suspend fun deleteById(tmdbId: Int, type: MediaType)
}

@Dao
interface EpisodeDao {

    @Query("SELECT * FROM episodes_watched WHERE tmdbId = :showId ORDER BY season, episode")
    fun observeForShow(showId: Int): Flow<List<EpisodeWatchedEntity>>

    @Query("SELECT * FROM episodes_watched WHERE syncState = 'PENDING_UPLOAD'")
    suspend fun pendingUploads(): List<EpisodeWatchedEntity>

    @Upsert
    suspend fun upsert(ep: EpisodeWatchedEntity)

    @Query("UPDATE episodes_watched SET syncState = 'SYNCED' WHERE tmdbId = :showId AND season = :season AND episode = :episode")
    suspend fun markSynced(showId: Int, season: Int, episode: Int)
}

@Dao
interface PendingOpsDao {
    @Insert suspend fun enqueue(op: PendingOpEntity)
    @Query("SELECT * FROM pending_ops ORDER BY createdAt ASC")
    suspend fun all(): List<PendingOpEntity>
    @Delete suspend fun delete(op: PendingOpEntity)
}
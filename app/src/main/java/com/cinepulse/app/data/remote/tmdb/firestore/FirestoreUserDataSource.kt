// data/remote/firestore/FirestoreUserDataSource.kt
package com.cinepulse.app.data.remote.tmdb.firestore

import com.cinepulse.app.data.local.entity.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreUserDataSource(
    private val firestore: FirebaseFirestore,
    private val uidProvider: () -> String?
) {
    private fun userMediaCol(uid: String) =
        firestore.collection("users").document(uid).collection("userMedia")

    private fun episodesCol(uid: String) =
        firestore.collection("users").document(uid).collection("episodesWatched")

    suspend fun upsertUserMedia(item: UserMediaEntity) {
        val uid = uidProvider() ?: return
        val docId = "${item.mediaType.name}_${item.tmdbId}"
        val data = mapOf(
            "tmdbId" to item.tmdbId,
            "mediaType" to item.mediaType.name,
            "title" to item.title,
            "posterPath" to item.posterPath,
            "status" to item.status.name,
            "rating" to item.rating,
            "review" to item.review,
            "updatedAt" to item.updatedAt
        )
        userMediaCol(uid).document(docId).set(data, SetOptions.merge()).await()
    }

    suspend fun upsertEpisode(ep: EpisodeWatchedEntity) {
        val uid = uidProvider() ?: return
        val docId = "${ep.tmdbId}_S${ep.season}E${ep.episode}"
        val data = mapOf(
            "tmdbId" to ep.tmdbId,
            "season" to ep.season,
            "episode" to ep.episode,
            "watchedAt" to ep.watchedAt
        )
        episodesCol(uid).document(docId).set(data, SetOptions.merge()).await()
    }
}
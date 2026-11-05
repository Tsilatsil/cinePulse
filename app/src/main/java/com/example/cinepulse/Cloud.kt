package com.example.cinepulse

import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import org.json.JSONObject

/**
 * All Cloud Firestore reads/writes live here.
 *
 * Database layout:
 *   users/{uid}                           name, email, points, fcmToken
 *   users/{uid}/watchlist/{movieKey}      movieJson, watched, archived, addedAt
 *   movies/{movieKey}/ratings/{uid}       rating (1-5), userName
 *   movies/{movieKey}/comments/{id}       uid, userName, text, createdAt
 *   recommendations/{id}                  uid, userName, movieJson, note, createdAt
 */
object Cloud {
    private const val TAG = "Cloud"

    private fun db() = FirebaseFirestore.getInstance()
    private fun userDoc(uid: String) = db().collection("users").document(uid)
    private fun movieDoc(key: String) = db().collection("movies").document(key)

    // ---------------------------- PROFILE ----------------------------

    /** Saves name, email and reward points on the user's profile document. */
    fun saveProfile(uid: String, points: Int) {
        val data = hashMapOf<String, Any>(
            "name" to Session.userName(),
            "email" to Session.email(),
            "points" to points,
            "updatedAt" to System.currentTimeMillis()
        )
        userDoc(uid).set(data, SetOptions.merge())
            .addOnFailureListener { Log.w(TAG, "saveProfile failed", it) }
    }

    /** Stores the FCM token so a push message can be sent to this phone. */
    fun saveToken(uid: String, token: String) {
        userDoc(uid).set(hashMapOf<String, Any>("fcmToken" to token), SetOptions.merge())
            .addOnSuccessListener { Log.d(TAG, "FCM token saved") }
            .addOnFailureListener { Log.w(TAG, "saveToken failed", it) }
    }

    /** Reads the saved reward points (used to restore progress after reinstalling). */
    fun pullPoints(uid: String, cb: (Int?) -> Unit) {
        userDoc(uid).get()
            .addOnSuccessListener { cb(it.getLong("points")?.toInt()) }
            .addOnFailureListener { cb(null) }
    }

    // ---------------------------- WATCHLIST ----------------------------

    fun pushWatchItem(uid: String, item: WatchItem, cb: (Boolean) -> Unit) {
        val data = hashMapOf<String, Any>(
            "movieJson" to item.movie.toJson().toString(),
            "watched" to item.watched,
            "archived" to item.archived,
            "addedAt" to item.addedAt
        )
        userDoc(uid).collection("watchlist").document(item.movie.key).set(data)
            .addOnSuccessListener { cb(true) }
            .addOnFailureListener { Log.w(TAG, "pushWatchItem failed", it); cb(false) }
    }

    fun deleteWatchItem(uid: String, key: String, cb: (Boolean) -> Unit) {
        userDoc(uid).collection("watchlist").document(key).delete()
            .addOnSuccessListener { cb(true) }
            .addOnFailureListener { Log.w(TAG, "deleteWatchItem failed", it); cb(false) }
    }

    /** Downloads the whole watchlist (used right after login to restore data). */
    fun pullWatchlist(uid: String, cb: (List<WatchItem>?) -> Unit) {
        userDoc(uid).collection("watchlist").get()
            .addOnSuccessListener { snap ->
                val items = snap.documents.mapNotNull { d ->
                    try {
                        val json = d.getString("movieJson") ?: return@mapNotNull null
                        WatchItem(
                            movie = Movie.fromJson(JSONObject(json)),
                            watched = d.getBoolean("watched") == true,
                            archived = d.getBoolean("archived") == true,
                            addedAt = d.getLong("addedAt") ?: 0L
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Bad watchlist document ${d.id}", e)
                        null
                    }
                }
                cb(items)
            }
            .addOnFailureListener { Log.w(TAG, "pullWatchlist failed", it); cb(null) }
    }

    // ---------------------------- RATINGS ----------------------------

    fun pushRating(uid: String, movieKey: String, rating: Float, cb: (Boolean) -> Unit) {
        val data = hashMapOf<String, Any>(
            "uid" to uid,
            "userName" to Session.userName(),
            "rating" to rating.toDouble(),
            "updatedAt" to System.currentTimeMillis()
        )
        movieDoc(movieKey).collection("ratings").document(uid).set(data)
            .addOnSuccessListener { cb(true) }
            .addOnFailureListener { Log.w(TAG, "pushRating failed", it); cb(false) }
    }

    /** Average rating from all CinePulse users, plus this user's own rating. */
    fun getCommunityRating(movieKey: String, uid: String, cb: (CommunityRating?) -> Unit) {
        movieDoc(movieKey).collection("ratings").get()
            .addOnSuccessListener { snap ->
                var sum = 0.0
                var count = 0
                var mine: Float? = null
                for (d in snap.documents) {
                    val r = d.getDouble("rating") ?: continue
                    sum += r
                    count++
                    if (d.id == uid) mine = r.toFloat()
                }
                cb(CommunityRating(if (count > 0) sum / count else 0.0, count, mine))
            }
            .addOnFailureListener { Log.w(TAG, "getCommunityRating failed", it); cb(null) }
    }

    // ---------------------------- COMMENTS ----------------------------

    /** The document id is built from uid + time, so re-sending the same comment never duplicates it. */
    fun pushComment(uid: String, movieKey: String, text: String, createdAt: Long, cb: (Boolean) -> Unit) {
        val data = hashMapOf<String, Any>(
            "uid" to uid,
            "userName" to Session.userName(),
            "text" to text,
            "createdAt" to createdAt
        )
        movieDoc(movieKey).collection("comments").document("c_${uid}_$createdAt").set(data)
            .addOnSuccessListener { cb(true) }
            .addOnFailureListener { Log.w(TAG, "pushComment failed", it); cb(false) }
    }

    /** REAL-TIME listener: the comment list updates the moment someone posts. Remove it in onDestroy. */
    fun listenComments(movieKey: String, onChange: (List<Comment>) -> Unit): ListenerRegistration =
        movieDoc(movieKey).collection("comments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) {
                    Log.w(TAG, "listenComments failed", err)
                    return@addSnapshotListener
                }
                onChange(snap.documents.map {
                    Comment(
                        userName = it.getString("userName") ?: "CinePulse user",
                        text = it.getString("text") ?: "",
                        createdAt = it.getLong("createdAt") ?: 0L
                    )
                })
            }

    // ---------------------------- RECOMMENDATIONS ----------------------------

    fun pushRecommendation(
        uid: String, movie: Movie, note: String, createdAt: Long, cb: (Boolean) -> Unit
    ) {
        val data = hashMapOf<String, Any>(
            "uid" to uid,
            "userName" to Session.userName(),
            "movieKey" to movie.key,
            "movieJson" to movie.toJson().toString(),
            "note" to note,
            "createdAt" to createdAt
        )
        db().collection("recommendations").document("r_${uid}_$createdAt").set(data)
            .addOnSuccessListener { cb(true) }
            .addOnFailureListener { Log.w(TAG, "pushRecommendation failed", it); cb(false) }
    }

    private fun parseRecommendation(d: DocumentSnapshot): Recommendation? {
        return try {
            Recommendation(
                uid = d.getString("uid") ?: "",
                movie = Movie.fromJson(JSONObject(d.getString("movieJson") ?: return null)),
                userName = d.getString("userName") ?: "CinePulse user",
                note = d.getString("note") ?: "",
                createdAt = d.getLong("createdAt") ?: 0L
            )
        } catch (e: Exception) {
            Log.w(TAG, "Bad recommendation ${d.id}", e)
            null
        }
    }

    /** Latest community recommendations for the Home screen. */
    fun getRecommendations(limit: Long, cb: (List<Recommendation>?) -> Unit) {
        db().collection("recommendations")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener { snap -> cb(snap.documents.mapNotNull { parseRecommendation(it) }) }
            .addOnFailureListener { Log.w(TAG, "getRecommendations failed", it); cb(null) }
    }

    /**
     * REAL-TIME alert: calls onNew when ANOTHER user posts a recommendation
     * while the app is open. Used to show a notification.
     */
    fun listenNewRecommendations(myUid: String, onNew: (Recommendation) -> Unit): ListenerRegistration {
        val startedAt = System.currentTimeMillis()
        return db().collection("recommendations")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                for (change in snap.documentChanges) {
                    if (change.type != DocumentChange.Type.ADDED) continue
                    val rec = parseRecommendation(change.document) ?: continue
                    if (rec.uid != myUid && rec.createdAt > startedAt) onNew(rec)
                }
            }
    }
}
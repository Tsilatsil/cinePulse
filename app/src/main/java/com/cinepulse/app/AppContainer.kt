// AppContainer.kt
package com.cinepulse.app

import android.app.Application
import android.content.Context
import com.cinepulse.app.data.local.CinePulseDatabase
import com.cinepulse.app.data.remote.tmdb.firestore.FirestoreUserDataSource
import com.cinepulse.app.data.remote.tmdb.TmdbApi
import com.cinepulse.app.data.remote.tmdb.TmdbModule
import com.cinepulse.app.data.repository.SettingsRepository
import com.cinepulse.app.data.repository.UserMediaRepository
import com.cinepulse.app.ui.auth.AuthManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AppContainer(context: Context) {
    private val db = CinePulseDatabase.get(context)
    val authManager = AuthManager()
    val settingsRepo = SettingsRepository(context)
    private val firestore = FirebaseFirestore.getInstance()

    val tmdb: TmdbApi = TmdbModule.create()

    private val firestoreUserSource = FirestoreUserDataSource(firestore) {
        authManager.getCurrentUid()
    }

    val repo = UserMediaRepository(db, firestoreUserSource)
}

class CinePulseApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

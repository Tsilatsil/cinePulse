package com.example.cinepulse

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate

/**
 * Runs once when the app process starts:
 *  - creates the notification channel
 *  - re-applies the saved dark/light mode
 *  - watches the connection: when the phone comes back online, offline changes are synced
 */
class CinePulseApp : Application() {

    companion object {
        private const val TAG = "CinePulseApp"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var wasOnline = false

    override fun onCreate() {
        super.onCreate()
        Notifier.createChannel(this)
        applySavedTheme()
        watchConnection()
    }

    private fun applySavedTheme() {
        val prefs = AppPrefs.get(this)
        if (prefs.contains(AppPrefs.KEY_DARK)) {
            val dark = prefs.getBoolean(AppPrefs.KEY_DARK, false)
            AppCompatDelegate.setDefaultNightMode(
                if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    /** OFFLINE SYNC trigger: fires when the internet comes back. */
    private fun watchConnection() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    val online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    if (online && !wasOnline) {
                        Log.d(TAG, "Internet is back - syncing offline changes")
                        // small delay so Android finishes switching networks
                        handler.postDelayed({
                            SyncManager.syncAll(applicationContext, announce = true)
                        }, 1500)
                    }
                    wasOnline = online
                }

                override fun onLost(network: Network) {
                    wasOnline = false
                    Log.d(TAG, "Internet lost - offline mode")
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Could not watch the connection", e)
        }
    }
}
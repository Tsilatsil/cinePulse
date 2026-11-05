package com.example.cinepulse

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.cinepulse.databinding.ActivityMainBinding
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Hosts the four main tabs (Home, Trending, Watchlist, Profile) and the bottom navigation.
 * Also:
 *  - shows an "offline" banner when there is no internet
 *  - asks for the notification permission (Android 13+)
 *  - saves the FCM token so push notifications can reach this phone
 *  - listens in real time for recommendations from other users
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val KEY_TAB = "current_tab"
        const val TAG_HOME = "home"
        const val TAG_TRENDING = "trending"
        const val TAG_WATCHLIST = "watchlist"
        const val TAG_PROFILE = "profile"
        private val ALL_TAGS = listOf(TAG_HOME, TAG_TRENDING, TAG_WATCHLIST, TAG_PROFILE)
    }

    private lateinit var b: ActivityMainBinding
    private var currentTag = TAG_HOME
    private var recListener: ListenerRegistration? = null

    private val connectivity by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> Log.d(TAG, "Notification permission granted = $granted") }

    // Watches the connection so the offline banner appears / disappears live
    private val netCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            runOnUiThread { setBanner(online = true) }
        }

        override fun onLost(network: Network) {
            runOnUiThread { setBanner(online = false) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Safety: if the user is not signed in, go back to Login
        if (!Session.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.bottomNav.setOnItemSelectedListener { item ->
            showTab(
                when (item.itemId) {
                    R.id.nav_trending -> TAG_TRENDING
                    R.id.nav_watchlist -> TAG_WATCHLIST
                    R.id.nav_profile -> TAG_PROFILE
                    else -> TAG_HOME
                }
            )
            true
        }

        if (savedInstanceState == null) {
            // First launch: add all four tabs once, show only Home
            val trending = TrendingFragment()
            val watchlist = WatchlistFragment()
            val profile = ProfileFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, HomeFragment(), TAG_HOME)
                .add(R.id.fragmentContainer, trending, TAG_TRENDING)
                .add(R.id.fragmentContainer, watchlist, TAG_WATCHLIST)
                .add(R.id.fragmentContainer, profile, TAG_PROFILE)
                .hide(trending)
                .hide(watchlist)
                .hide(profile)
                .commit()
        } else {
            // Screen rotated / language changed: restore the selected tab
            currentTag = savedInstanceState.getString(KEY_TAB) ?: TAG_HOME
            b.bottomNav.selectedItemId = idForTag(currentTag)
        }

        requestNotificationPermission()
        saveFcmToken()
    }

    override fun onStart() {
        super.onStart()
        if (!::b.isInitialized) return

        setBanner(NetworkUtil.isOnline(this))
        try {
            connectivity.registerDefaultNetworkCallback(netCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Could not register network callback", e)
        }

        // Push any changes made offline
        SyncManager.syncAll(applicationContext)

        // REAL-TIME alert: someone else recommended a title
        recListener = Cloud.listenNewRecommendations(Session.uid()) { rec ->
            Notifier.show(
                this,
                getString(R.string.notif_new_rec_title, rec.userName),
                rec.movie.title
            )
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            connectivity.unregisterNetworkCallback(netCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Network callback was not registered", e)
        }
        recListener?.remove()
        recListener = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_TAB, currentTag)
    }

    /** Called by fragments (e.g. the "See all" link on Home) to change tab. */
    fun selectTab(menuId: Int) {
        b.bottomNav.selectedItemId = menuId
    }

    private fun showTab(tag: String) {
        val tx = supportFragmentManager.beginTransaction()
        for (t in ALL_TAGS) {
            val fragment = supportFragmentManager.findFragmentByTag(t) ?: continue
            if (t == tag) tx.show(fragment) else tx.hide(fragment)
        }
        tx.commit()
        currentTag = tag
    }

    private fun idForTag(tag: String) = when (tag) {
        TAG_TRENDING -> R.id.nav_trending
        TAG_WATCHLIST -> R.id.nav_watchlist
        TAG_PROFILE -> R.id.nav_profile
        else -> R.id.nav_home
    }

    private fun setBanner(online: Boolean) {
        b.tvOffline.visibility = if (online) View.GONE else View.VISIBLE
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** The FCM token identifies this phone for push notifications. */
    private fun saveFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d(TAG, "FCM token: $token")
                Cloud.saveToken(Session.uid(), token)
            }
            .addOnFailureListener { Log.w(TAG, "Could not get FCM token", it) }
    }
}
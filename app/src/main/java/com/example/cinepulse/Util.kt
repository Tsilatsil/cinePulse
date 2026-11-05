package com.example.cinepulse

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executors

/** Runs database work on a background thread and returns the result on the main thread. */
object Bg {
    private const val TAG = "Bg"
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    /** Do [work] in the background, then call [done] on the main thread. */
    fun <T> run(work: () -> T, done: (T) -> Unit) {
        io.execute {
            try {
                val result = work()
                main.post { done(result) }
            } catch (e: Exception) {
                Log.e(TAG, "Background work failed", e)
            }
        }
    }

    /** Fire-and-forget background work. */
    fun exec(work: () -> Unit) {
        io.execute {
            try {
                work()
            } catch (e: Exception) {
                Log.e(TAG, "Background work failed", e)
            }
        }
    }

    fun post(block: () -> Unit) {
        main.post { block() }
    }
}

/** Checks whether the phone currently has a working internet connection. */
object NetworkUtil {
    fun isOnline(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

/** Quick access to the signed-in Firebase user. */
object Session {
    fun uid(): String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    fun email(): String = FirebaseAuth.getInstance().currentUser?.email ?: ""

    fun userName(): String {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")
            ?: "CinePulse user"
    }

    fun isLoggedIn(): Boolean = FirebaseAuth.getInstance().currentUser != null
}
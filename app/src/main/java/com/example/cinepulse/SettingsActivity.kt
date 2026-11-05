package com.example.cinepulse

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.cinepulse.databinding.ActivitySettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Settings: dark mode, language (English / Afrikaans / isiZulu), notifications on/off,
 * offline sync status + "Sync now", push token and log out.
 */
class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SettingsActivity"
    }

    private lateinit var b: ActivitySettingsBinding
    private val app by lazy { applicationContext }
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        b.tvAccount.text = Session.email().ifEmpty { Session.userName() }
        b.tvVersion.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)

        setupDarkMode()
        setupLanguage()
        setupNotifications()
        setupSync()
        setupToken()

        b.btnLogout.setOnClickListener { confirmLogout() }
    }

    override fun onResume() {
        super.onResume()
        refreshSync()
    }

    // ------------------------------------------------------------------
    // Dark mode (saved, re-applied on every app start by CinePulseApp)
    // ------------------------------------------------------------------
    private fun setupDarkMode() {
        val prefs = AppPrefs.get(this)
        val isDark = if (prefs.contains(AppPrefs.KEY_DARK)) {
            prefs.getBoolean(AppPrefs.KEY_DARK, false)
        } else {
            // not chosen yet: show what the phone is currently using
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
        }
        b.switchDarkMode.isChecked = isDark
        b.switchDarkMode.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(AppPrefs.KEY_DARK, checked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    // ------------------------------------------------------------------
    // MULTI-LANGUAGE: English, Afrikaans and isiZulu
    // ------------------------------------------------------------------
    private fun currentLanguage(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        val lang = if (!locales.isEmpty) locales[0]?.language
        else resources.configuration.locales[0].language
        return if (lang == "af" || lang == "zu") lang else "en"
    }

    private fun setupLanguage() {
        b.toggleLanguage.check(
            when (currentLanguage()) {
                "af" -> R.id.btnLangAf
                "zu" -> R.id.btnLangZu
                else -> R.id.btnLangEn
            }
        )
        b.toggleLanguage.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val tag = when (checkedId) {
                R.id.btnLangAf -> "af"
                R.id.btnLangZu -> "zu"
                else -> "en"
            }
            if (tag != currentLanguage()) {
                Log.d(TAG, "Changing language to $tag")
                // Android restarts the screens and loads values-af / values-zu automatically
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            }
        }
    }

    // ------------------------------------------------------------------
    // Notifications
    // ------------------------------------------------------------------
    private fun setupNotifications() {
        b.switchNotifications.isChecked = Notifier.isEnabled(this)
        b.switchNotifications.setOnCheckedChangeListener { _, checked ->
            Notifier.setEnabled(this, checked)
        }
    }

    // ------------------------------------------------------------------
    // Offline sync
    // ------------------------------------------------------------------
    private fun setupSync() {
        b.btnSyncNow.setOnClickListener {
            if (!NetworkUtil.isOnline(this)) {
                Toast.makeText(this, R.string.err_network, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            b.btnSyncNow.isEnabled = false
            SyncManager.syncAll(app) { count ->
                if (isFinishing || isDestroyed) return@syncAll
                b.btnSyncNow.isEnabled = true
                Toast.makeText(
                    this,
                    if (count > 0) getString(R.string.sync_done_toast, count)
                    else getString(R.string.sync_nothing),
                    Toast.LENGTH_SHORT
                ).show()
                refreshSync()
            }
        }
    }

    private fun refreshSync() {
        val uid = Session.uid()
        Bg.run({ LocalDb.get(app).pendingCount(uid) }) { pending ->
            if (isFinishing || isDestroyed) return@run
            b.tvSyncStatus.text = when {
                pending == 0 -> getString(R.string.settings_sync_clean)
                !NetworkUtil.isOnline(this) -> getString(R.string.settings_sync_offline, pending)
                else -> getString(R.string.settings_sync_pending, pending)
            }
        }
    }

    // ------------------------------------------------------------------
    // Push notification token (for testing from the Firebase console)
    // ------------------------------------------------------------------
    private fun setupToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { t ->
                token = t
                b.tvToken.text = t
            }
            .addOnFailureListener {
                Log.w(TAG, "Could not get FCM token", it)
                b.tvToken.setText(R.string.settings_token_unavailable)
            }

        b.btnCopyToken.setOnClickListener {
            val t = token ?: return@setOnClickListener
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("FCM token", t))
            Toast.makeText(this, R.string.settings_token_copied, Toast.LENGTH_SHORT).show()
        }
    }

    // ------------------------------------------------------------------
    // Log out
    // ------------------------------------------------------------------
    private fun confirmLogout() {
        val uid = Session.uid()
        Bg.run({ LocalDb.get(app).pendingCount(uid) }) { pending ->
            when {
                pending == 0 -> doLogout()
                NetworkUtil.isOnline(this) -> SyncManager.syncAll(app) { doLogout() }
                else -> MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.btn_logout)
                    .setMessage(getString(R.string.logout_unsynced, pending))
                    .setPositiveButton(R.string.btn_logout) { _, _ -> doLogout() }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun doLogout() {
        Log.d(TAG, "Logging out")
        FirebaseAuth.getInstance().signOut()
        // Clear the back stack so Back cannot return to the app after logging out
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}
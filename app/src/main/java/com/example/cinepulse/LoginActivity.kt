package com.example.cinepulse

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.cinepulse.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest

/**
 * Login + Register screen with Firebase Authentication.
 *
 * Password security: the password is sent over HTTPS (TLS) and Firebase stores only a
 * salted, hashed version (scrypt) - the app never stores or logs the plain password.
 *
 * SSO: "Continue with Google" uses Google Sign-In, then exchanges the Google ID token
 * for a Firebase credential (single sign-on).
 */
class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"
    }

    private lateinit var b: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleClient: GoogleSignInClient
    private var registerMode = false

    // Receives the result of the Google account chooser
    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken == null) {
                setBusy(false)
                showError(getString(R.string.err_google_failed))
            } else {
                firebaseAuthWithGoogle(idToken)
            }
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign-in failed, code=${e.statusCode}", e)
            setBusy(false)
            when (e.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> { /* user closed the chooser */ }
                10 -> showError(getString(R.string.err_google_config)) // DEVELOPER_ERROR: SHA-1 missing
                else -> showError(getString(R.string.err_google_failed))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        // Already signed in? Skip straight to the app.
        if (auth.currentUser != null) {
            goMain()
            return
        }

        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Configure Google Sign-In (default_web_client_id is generated from google-services.json)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)

        setMode(false)

        b.btnPrimary.setOnClickListener { submit() }
        b.btnToggle.setOnClickListener { setMode(!registerMode) }
        b.btnGoogle.setOnClickListener { startGoogleSignIn() }
    }

    /** Switches the form between Login and Register. */
    private fun setMode(register: Boolean) {
        registerMode = register
        clearErrors()
        b.tilName.visibility = if (register) View.VISIBLE else View.GONE
        b.tvFormTitle.setText(if (register) R.string.register_title else R.string.login_title)
        b.btnPrimary.setText(if (register) R.string.btn_register else R.string.btn_login)
        b.btnToggle.setText(if (register) R.string.toggle_to_login else R.string.toggle_to_register)
    }

    /** Validates the input so invalid values never reach Firebase (and never crash the app). */
    private fun validate(): Boolean {
        clearErrors()
        var ok = true
        val name = b.etName.text.toString().trim()
        val email = b.etEmail.text.toString().trim()
        val password = b.etPassword.text.toString()

        if (registerMode && name.length < 2) {
            b.tilName.error = getString(R.string.err_name_short)
            ok = false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            b.tilEmail.error = getString(R.string.err_email_invalid)
            ok = false
        }
        if (password.length < 6) {
            b.tilPassword.error = getString(R.string.err_password_short)
            ok = false
        }
        return ok
    }

    private fun submit() {
        if (!validate()) return
        if (!NetworkUtil.isOnline(this)) {
            showError(getString(R.string.err_network))
            return
        }
        val name = b.etName.text.toString().trim()
        val email = b.etEmail.text.toString().trim()
        val password = b.etPassword.text.toString()

        setBusy(true)
        if (registerMode) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Registered new user")
                        // Save the chosen username on the Firebase profile
                        val request = UserProfileChangeRequest.Builder().setDisplayName(name).build()
                        val user = auth.currentUser
                        if (user != null) {
                            user.updateProfile(request).addOnCompleteListener { onAuthSuccess() }
                        } else {
                            onAuthSuccess()
                        }
                    } else {
                        onAuthFailed(task.exception)
                    }
                }
        } else {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Email login successful")
                        onAuthSuccess()
                    } else {
                        onAuthFailed(task.exception)
                    }
                }
        }
    }

    private fun startGoogleSignIn() {
        if (!NetworkUtil.isOnline(this)) {
            showError(getString(R.string.err_network))
            return
        }
        clearErrors()
        setBusy(true)
        // Sign out first so the account chooser always appears
        googleClient.signOut().addOnCompleteListener {
            googleLauncher.launch(googleClient.signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Google SSO login successful")
                onAuthSuccess()
            } else {
                onAuthFailed(task.exception)
            }
        }
    }

    private fun onAuthSuccess() {
        // Restore the user's data from Firestore and push any offline changes
        SyncManager.onLogin(applicationContext)
        goMain()
    }

    private fun onAuthFailed(e: Exception?) {
        Log.w(TAG, "Authentication failed", e)
        setBusy(false)
        val message = when (e) {
            is FirebaseAuthInvalidUserException,
            is FirebaseAuthInvalidCredentialsException -> R.string.err_invalid_credentials
            is FirebaseAuthUserCollisionException -> R.string.err_user_exists
            is FirebaseAuthWeakPasswordException -> R.string.err_password_short
            is FirebaseNetworkException -> R.string.err_network
            else -> R.string.err_generic
        }
        showError(getString(message))
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // ---------- small UI helpers ----------

    private fun setBusy(busy: Boolean) {
        b.progress.visibility = if (busy) View.VISIBLE else View.GONE
        b.btnPrimary.isEnabled = !busy
        b.btnGoogle.isEnabled = !busy
        b.btnToggle.isEnabled = !busy
    }

    private fun showError(message: String) {
        b.tvStatus.text = message
        b.tvStatus.visibility = View.VISIBLE
    }

    private fun clearErrors() {
        b.tilName.error = null
        b.tilEmail.error = null
        b.tilPassword.error = null
        b.tvStatus.visibility = View.GONE
    }
}
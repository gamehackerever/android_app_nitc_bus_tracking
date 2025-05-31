package com.nitc.nitcbustracker

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.checkbox.MaterialCheckBox
import com.nitc.nitcbustracker.data.model.LoginRequest
import kotlinx.coroutines.*
import java.io.IOException
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var rememberMeCheckbox: MaterialCheckBox

    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "LoginPrefs"

    private val RC_SIGN_IN = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)

        // Views
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        val btnGoogleSignIn = findViewById<SignInButton>(R.id.btnGoogleSignIn)

        rememberMeCheckbox = findViewById(R.id.cbRememberMe)

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Load saved credentials
        val savedUsername = sharedPreferences.getString("email", null)
        val savedPassword = sharedPreferences.getString("password", null)

        if (savedUsername != null && savedPassword != null) {
            // Autofill the fields
            etUsername.setText(savedUsername)
            etPassword.setText(savedPassword)
            rememberMeCheckbox.isChecked = true
        }

        // Google Sign-In setup
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            // User is already signed in
            val intent = Intent(this, StudentActivity::class.java)
            Toast.makeText(this@LoginActivity, "Welcome back, ${account.displayName}", Toast.LENGTH_SHORT).show()
            startActivity(intent)
            finish()
        }

        FirebaseMessaging.getInstance().subscribeToTopic("notifications")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Subscribed to notifications")
                }
            }

        btnGoogleSignIn.setOnClickListener {
            googleSignInClient.signOut()
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        btnLogin.setOnClickListener {
            val email = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (rememberMeCheckbox.isChecked) {
                // Save credentials
                sharedPreferences.edit {
                    putString("email", email)
                        .putString("password", password)
                }
            } else {
                // Clear credentials if unchecked
                sharedPreferences.edit()
                    .remove("email")
                    .remove("password")
                    .apply()
            }

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(email, password)
            }
        }

    }

    private fun loginUser(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.login(LoginRequest(email, password))
                }
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse?.success == true) {
                        when (loginResponse.role) {
                            "admin" -> {
                                startActivity(Intent(this@LoginActivity, AdminActivity::class.java))
                                finish()
                            }
                            "driver" -> {
                                val intent = Intent(this@LoginActivity, DriverActivity::class.java)
                                intent.putExtra("busId", loginResponse.busId)
                                startActivity(intent)
                                finish()
                            }
                            "student" -> {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Student login is only available via Google Sign-In",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Unknown role",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Toast.makeText(this@LoginActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                val email = account.email
                val name = account.displayName

                if (email != null) {
                    // Check with server
                    lifecycleScope.launch {
                        try {
                            val response = withContext(Dispatchers.IO) {
                                RetrofitClient.api.checkUserExists(email)
                            }

                            if (response.isSuccessful) {
                                val exists = response.body()?.get("exists") == true
                                if (exists) {
                                    // User exists → go to StudentActivity
                                    startActivity(Intent(this@LoginActivity, StudentActivity::class.java))
                                    Toast.makeText(this@LoginActivity, "Welcome back, ${account.displayName}", Toast.LENGTH_SHORT).show()
                                } else if (email.endsWith("@nitc.ac.in")) {
                                    // Auto-add user and go to RegisterActivity
                                    try {
                                        val createResponse = withContext(Dispatchers.IO) {
                                            RetrofitClient.api.partialRegistration(mapOf("email" to email))
                                        }

                                        if (createResponse.isSuccessful) {
                                            Toast.makeText(this@LoginActivity, "Welcome back, ${account.displayName}", Toast.LENGTH_SHORT).show()
                                            val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
                                            intent.putExtra("prefill_name", name)
                                            intent.putExtra("prefill_email", email)
                                            startActivity(intent)
                                        } else {
                                            Toast.makeText(this@LoginActivity, "Couldn't auto-register", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(this@LoginActivity, "Error registering: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(this@LoginActivity, "Only NITC emails are allowed", Toast.LENGTH_SHORT).show()
                                }
                            }


                        } catch (e: Exception) {
                            Toast.makeText(this@LoginActivity, "Server error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "Sign-in failed: ${e.statusCode}")
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

}

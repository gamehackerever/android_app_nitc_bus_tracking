package com.nitc.nitcbustracker

import RetrofitClient.api
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import com.google.android.gms.auth.api.signin.GoogleSignInClient


class ProfileFragment : Fragment() {

    lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val profileName = view.findViewById<EditText>(R.id.profile_name)
        val profileEmail = view.findViewById<EditText>(R.id.profile_email)
        val profilePhoneNo = view.findViewById<EditText>(R.id.profile_phone_no)
        val profileHostel = view.findViewById<EditText>(R.id.profile_hostel)
        val profilePhoto = view.findViewById<ImageView>(R.id.profile_image)
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(requireContext())
        val email = account?.email
        val photo_url = account?.photoUrl

        // Initialize GoogleSignInClient
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        val logoutButton = view.findViewById<Button>(R.id.logout_button)

        logoutButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { _, _ ->
                    googleSignInClient.signOut().addOnCompleteListener {
                        val intent = Intent(requireActivity(), LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }


        // Load profile photo manually
        if (photo_url != null) {
            lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        val inputStream = URL(photo_url.toString()).openStream()
                        BitmapFactory.decodeStream(inputStream)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    profilePhoto.setImageBitmap(bitmap)
                }
            }
        }

        // Launch coroutine here using lifecycleScope
        lifecycleScope.launch {
            val response = api.getUserInfo(email.toString())
            if (response.isSuccessful) {
                val userInfo = response.body()

                profileName.setText(userInfo?.name)
                profileEmail.setText(userInfo?.email)
                profilePhoneNo.setText(userInfo?.phone)
                profileHostel.setText(userInfo?.hostel)
                profilePhoto.setImageURI(photo_url)
            } else {
                profileName.setText("Default Name")
                profileEmail.setText("Default Email")
                profilePhoneNo.setText("Default Phone Number")
                profileHostel.setText("Default Hostel")
            }
        }
    }
}

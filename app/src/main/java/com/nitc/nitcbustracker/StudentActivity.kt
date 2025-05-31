package com.nitc.nitcbustracker

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.messaging.FirebaseMessaging

class StudentActivity : AppCompatActivity() {

    private lateinit var mapFragment: MapFragment
    private lateinit var profileFragment: ProfileFragment
    private lateinit var getNoticeFragment: GetNoticeFragment
    private lateinit var bottomNavigationView: BottomNavigationView
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student)

        bottomNavigationView = findViewById(R.id.bottom_navigation_student)

        if (savedInstanceState == null) {
            mapFragment = MapFragment()
            profileFragment = ProfileFragment()
            getNoticeFragment = GetNoticeFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.nav_student_fragment, profileFragment, "PROFILE")
                .hide(profileFragment)
                .add(R.id.nav_student_fragment, mapFragment, "MAP")
                .add(R.id.nav_student_fragment, getNoticeFragment, "NOTICE")
                .hide(getNoticeFragment)
                .commit()

            activeFragment = mapFragment
        } else {
            mapFragment = supportFragmentManager.findFragmentByTag("MAP") as? MapFragment ?: MapFragment()
            profileFragment = supportFragmentManager.findFragmentByTag("PROFILE") as? ProfileFragment ?: ProfileFragment()
            getNoticeFragment = supportFragmentManager.findFragmentByTag("NOTICE") as? GetNoticeFragment ?: GetNoticeFragment()

            activeFragment = when (bottomNavigationView.selectedItemId) {
                R.id.nav_profile -> profileFragment
                R.id.nav_map -> mapFragment
                R.id.nav_notice -> getNoticeFragment
                else -> mapFragment
            }
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            val fragmentToShow = when (item.itemId) {
                R.id.nav_map -> mapFragment
                R.id.nav_profile -> profileFragment
                R.id.nav_notice -> getNoticeFragment
                else -> mapFragment
            }
            showFragment(fragmentToShow)
            true
        }
    }

    private fun showFragment(fragmentToShow: Fragment) {
        if (activeFragment == fragmentToShow) return

        supportFragmentManager.beginTransaction()
            .hide(activeFragment!!)
            .show(fragmentToShow)
            .commit()

        activeFragment = fragmentToShow
    }
}

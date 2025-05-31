package com.nitc.nitcbustracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var postNoticeFragment: PostNoticeFragment
    private lateinit var adminFragment: AdminFragment
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        bottomNavigationView = findViewById(R.id.bottom_navigation_admin)

        if (savedInstanceState == null) {
            postNoticeFragment = PostNoticeFragment()
            adminFragment = AdminFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.nav_admin_fragment, postNoticeFragment, "POST NOTICE")
                .add(R.id.nav_admin_fragment, adminFragment, "ADMIN")
                .hide(postNoticeFragment)
                .commit()

            activeFragment = adminFragment
        } else {
            postNoticeFragment = supportFragmentManager.findFragmentByTag("POST NOTICE") as? PostNoticeFragment ?: PostNoticeFragment()
            adminFragment = supportFragmentManager.findFragmentByTag("ADMIN") as? AdminFragment ?: AdminFragment()

            activeFragment = when (bottomNavigationView.selectedItemId) {
                R.id.nav_admin -> adminFragment
                R.id.nav_notice -> postNoticeFragment
                else -> adminFragment
            }
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            val fragmentToShow = when (item.itemId) {
                R.id.nav_admin -> adminFragment
                R.id.nav_notice -> postNoticeFragment
                else -> adminFragment
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

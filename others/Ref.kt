/*
 * Copyright (C) 2016-2017 Sandip Vaghela
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.afterroot.allusive.fragment

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.*
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.graphics.drawable.DrawerArrowDrawable
import android.transition.TransitionInflater
import android.view.MenuItem
import android.view.View
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.allusive.Helper
import com.afterroot.allusive.PermissionChecker
import com.afterroot.allusive.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_main.*

/**
 * Created by Sandip on 26-11-2017.
 */

class MainActivity: AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var mainFragment: MainFragment? = null
    private var customizeFragment: CustomizeFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        //Replace Launch theme with Light Theme
        setTheme(R.style.AppTheme_Light)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        //Firebase Analytics, logs every time when user starts activity.
        val analytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle()
        bundle.putString("Device_Name", Build.DEVICE)
        bundle.putString("AndroidVersion", Build.VERSION.RELEASE)
        analytics.logEvent("DeviceInfo", bundle)

        //Initialize Interstitial Ad
        MobileAds.initialize(this, getString(R.string.interstitial_ad_1_id))

        nav_view.setNavigationItemSelectedListener(this)

        navigation.setOnNavigationItemSelectedListener(bottomNavViewListener)

        val arrowDrawable = DrawerArrowDrawable(this)
        arrowDrawable!!.color =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    resources.getColor(android.R.color.white, theme)
                } else {
                    resources.getColor(android.R.color.white)
                }

        toolbar.navigationIcon = arrowDrawable

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            nav_view.apply {
                itemIconTintList = resources.getColorStateList(R.color.nav_state_list, theme)
                itemTextColor = resources.getColorStateList(R.color.nav_state_list, theme)
            }
            checkPermissions()
        } else {
            nav_view.apply {
                itemIconTintList = resources.getColorStateList(R.color.nav_state_list)
                itemTextColor = resources.getColorStateList(R.color.nav_state_list)
            }
            mainFragment = MainFragment.newInstance()
            Handler().postDelayed({
                addFragment(mainFragment!!, R.id.fragment_container)
            }, 100)
        }

        fab_tune.setOnClickListener {
            if (customizeFragment == null){
                customizeFragment = CustomizeFragment.newInstance()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val changeTransform = TransitionInflater.from(this).inflateTransition(R.transition.change_pointer_transform)
                val explode = TransitionInflater.from(this).inflateTransition(R.transition.explode)

                customizeFragment!!.apply {
                    sharedElementEnterTransition = changeTransform
                    enterTransition = explode
                    exitTransition = explode
                    sharedElementReturnTransition = changeTransform
                }
                replaceFragment(customizeFragment!!, R.id.fragment_container) {
                    addToBackStack("sliders")
                    addSharedElement(selected_pointer,  ViewCompat.getTransitionName(selected_pointer))
                }
            } else {
                replaceFragment(customizeFragment!!, R.id.fragment_container) {
                    addToBackStack("sliders")
                }
            }

            ObjectAnimator.ofFloat(arrowDrawable, "progress", 0f,1f).start()
            toolbar.setNavigationOnClickListener {
                supportFragmentManager.popBackStack("sliders", FragmentManager.POP_BACK_STACK_INCLUSIVE)
                ObjectAnimator.ofFloat(arrowDrawable, "progress", 1f,0f).start()
                toolbar.setNavigationOnClickListener {
                    drawer_layout.openDrawer(GravityCompat.START)
                }
            }
        }
    }

    //BottomNavigationView listener
    private val bottomNavViewListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                replaceFragment(mainFragment!!, R.id.fragment_container)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_manage_pointer -> {
                replaceFragment(InstallPointerFragment(), R.id.fragment_container)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_settings -> {
                replaceFragment(SettingsFragment(), R.id.fragment_container)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    //NavigationDrawer item selected callback
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId){

        }
        return false
    }

    private val manifestPermissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE)

    private fun checkPermissions() {
        val permissionChecker = PermissionChecker(this)
        if (permissionChecker.lacksPermissions(manifestPermissions)) {
            ActivityCompat.requestPermissions(this, manifestPermissions, REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE -> {
                val isPermissionGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (!isPermissionGranted) {
                    Helper.showSnackBar(this.coordinator_layout, "Please Grant Permissions", Snackbar.LENGTH_INDEFINITE, "GRANT", View.OnClickListener { checkPermissions() })
                } else {
                    if (mainFragment == null) {
                        mainFragment = MainFragment()
                    }
                    addFragment(mainFragment!!, R.id.fragment_container)
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    public override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private var interstitialAd: InterstitialAd? = null
    fun setUpInterstitialAd(){
        interstitialAd = InterstitialAd(this)
        interstitialAd!!.apply {
            adUnitId = getString(R.string.interstitial_ad_1_id)
            loadAd(AdRequest.Builder().build())
            adListener = object : AdListener() {
                override fun onAdClosed() {
                    interstitialAd!!.loadAd(AdRequest.Builder().build())
                }
            }
        }
    }

    companion object {
        private val REQUEST_CODE = 0
        fun showInstallPointersDialog(context: Context) {
            MaterialDialog.Builder(context)
                    .title("Install Pointers")
                    .content("No Pointers installed. Please install some pointers")
                    .positiveText("Install Pointers")
                    .onPositive { _: MaterialDialog, _: DialogAction ->
                        TODO("Add Fragment")
                    }.show()
        }

        fun showInstallPointerFragment(activity: FragmentActivity){
            activity.supportFragmentManager.beginTransaction().replace(R.id.fragment_container, InstallPointerFragment()).commit()
        }
    }

    /**
     * @see <a href="https://medium.com/thoughts-overflow/how-to-add-a-fragment-in-kotlin-way-73203c5a450b">Source: How to Add a Fragment the Kotlin way</a></p>
     */
    private inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
        beginTransaction().setCustomAnimations(R.anim.slide_in, R.anim.slide_out, R.anim.slide_in, R.anim.slide_out).func().commit()
    }
    private fun AppCompatActivity.addFragment(fragment: Fragment, frameId: Int, func: (FragmentTransaction.() -> FragmentTransaction)? = null){
        if (func != null) {
            supportFragmentManager.inTransaction { add(frameId, fragment).func() }
        } else{
            supportFragmentManager.inTransaction { add(frameId, fragment) }
        }
    }

    private fun AppCompatActivity.replaceFragment(fragment: Fragment, frameId: Int, func: (FragmentTransaction.() -> FragmentTransaction)? = null) {
        if (func != null) {
            supportFragmentManager.inTransaction { replace(frameId, fragment).func() }
        } else {
            supportFragmentManager.inTransaction { replace(frameId, fragment) }
        }
    }
}
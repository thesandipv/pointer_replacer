/*
 * Copyright (C) 2016-2019 Sandip Vaghela
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

/*
package com.afterroot.pointerdash

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.allusive.R
import com.afterroot.allusive.fragment.InstallPointerFragment
import com.afterroot.allusive.fragment.MainFragment
import com.afterroot.allusive.fragment.PointersRepoFragment
import com.afterroot.allusive.fragment.SettingsFragment
import com.afterroot.allusive.utils.DatabaseFields
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.nav_header_main.*
import org.jetbrains.anko.design.indefiniteSnackbar

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val _tag = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        //Replace Launch theme with Light Theme
        setTheme(R.style.AppTheme_Light)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
    }


    override fun onResume() {
        super.onResume()

        toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle!!)
        toggle!!.syncState()

        //Firebase Analytics, logs every time when user starts activity.
        val analytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle()
        bundle.putString("Device_Name", Build.DEVICE)
        bundle.putString("Manufacturer", Build.MANUFACTURER)
        bundle.putString("AndroidVersion", Build.VERSION.RELEASE)
        analytics.logEvent("DeviceInfo", bundle)

        //TODO enable ads
        //Initialize Interstitial Ad
        //MobileAds.initialize(this, getString(R.string.interstitial_ad_1_id))

        nav_view.setNavigationItemSelectedListener(this)

        init()

    }

    private fun init() {

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

            loadFragments()

            if (Settings.System.getInt(contentResolver, "show_touches") == 0) {
                indefiniteSnackbar(view_pagerold, "Show touches disabled. Would you like to enable", "ENABLE", {
                    Settings.System.putInt(contentResolver,
                            "show_touches", 1)
                }).show()
            }
        }

        nav_view.getHeaderView(0)?.let {
            FirebaseAuth.getInstance().currentUser.let {
                if (it != null) {
                    header_username?.text = it.displayName
                    header_email?.text = it.email
                }
            }
        }

        val db = FirebaseFirestore.getInstance()

        FirebaseAuth.getInstance().currentUser.let {
            if (it != null) {
                db.collection(DatabaseFields.USERS)
                        .document(it.uid).set(User(it.displayName!!, it.email!!, it.uid))
            }
        }
    }

    private var viewpagerAdapter: BottomNavigationAdapter? = null
    private fun loadFragments() {
        view_pagerold.setPagingEnabled(false)
        viewpagerAdapter = BottomNavigationAdapter(supportFragmentManager)
        val mainFragment = MainFragment.newInstance()
        val installPointerFragment = InstallPointerFragment.newInstance()
        val settingsFragment = SettingsFragment()
        val pointersRepoFragment = RepoHolderFragment()

        viewpagerAdapter!!.run {
            addFragment(mainFragment, "Allusive")
            addFragment(pointersRepoFragment, "Browse Pointers")
            addFragment(settingsFragment, "Settings")
        }

        view_pagerold.adapter = viewpagerAdapter

        navigationold.setOnNavigationItemSelectedListener { item ->
            var title = getString(R.string.app_name)
            when (item.itemId) {
                R.id.navigation_home -> {
                    view_pagerold.currentItem = 0
                    title = viewpagerAdapter!!.getPageTitle(0).toString()
                }
                R.id.navigation_manage_pointer -> {
                    view_pagerold.currentItem = 1
                    title = viewpagerAdapter!!.getPageTitle(1).toString()
                }
                R.id.navigation_settings -> {
                    view_pagerold.currentItem = 2
                    title = viewpagerAdapter!!.getPageTitle(2).toString()
                }
            }
            toolbar.title = title
            return@setOnNavigationItemSelectedListener true
        }
    }

    private val manifestPermissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_SETTINGS)

    private fun checkPermissions() {
        Log.d(_tag, "checkPermissions: Checking Permissions..")
        val permissionChecker = PermissionChecker(this)
        if (permissionChecker.lacksPermissions(manifestPermissions)) {
            Log.d(_tag, "checkPermissions: Requesting Permissions..")
            ActivityCompat.requestPermissions(this, manifestPermissions, RC_PERMISSION)
        } else {
            Log.d(_tag, "checkPermissions: Permissions Granted..")
            loadFragments()

            //TODO
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    when {
                        Settings.System.canWrite(this) ->
                            when {
                                Settings.System.getInt(contentResolver, "show_touches") == 0 ->
                                    indefiniteSnackbar(view_pager, "Show touches disabled. Would you like to enable", "ENABLE", {
                                        Settings.System.putInt(contentResolver,
                                                "show_touches", 1)
                                    }).show()
                            }
                        else -> {
                            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                            intent.data = Uri.parse("package:$packageName")
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent)
                        }
                    }
            }

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            RC_PERMISSION -> {
                val isPermissionGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (!isPermissionGranted) {
                    Log.d(_tag, "onRequestPermissionsResult: Permissions not Granted..")
                    Helper.showSnackBar(this.coordinator_layout, "Please Grant Permissions", Snackbar.LENGTH_INDEFINITE, "GRANT", View.OnClickListener { checkPermissions() })
                } else {
                    checkPermissions()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PointersRepoFragment.rcSignIn) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {

            } else {
                if (response == null) {
                    toast("Sign In cancelled")
                    return
                }
                if (response.error!!.errorCode == ErrorCodes.NO_NETWORK) {
                    toast("No Network")
                    return
                }

                toast("Unknown Error")
                Log.e(_tag, "onActivityResult: Sign In Error", response.error)
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.reboot -> showRebootDialog()
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.title) {
            getString(R.string.text_edit_profile) -> {
                //TODO add fragment
                Navigation.findNavController(this, R.id.fragment_repo_nav).navigate(R.id.editProfileFragment)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp() =
            Navigation.findNavController(this, R.id.fragment_repo_nav).navigateUp()

    private fun showRebootDialog() {
        AlertDialog.Builder(this, R.style.Theme_AppCompat_DayNight_Dialog_Alert)
                .setTitle(R.string.reboot)
                .setMessage(R.string.text_reboot_confirm)
                .setPositiveButton(R.string.reboot) { _, _ ->
                    try {
                        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot"))
                        process.waitFor()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .setNegativeButton(R.string.text_no) { _, _ ->

                }
                .setNeutralButton(R.string.text_soft_reboot) { _, _ ->
                    try {
                        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "busybox killall system_server"))
                        process.waitFor()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.show()
    }

    companion object {
        var toggle: ActionBarDrawerToggle? = null
        private val RC_PERMISSION: Int = 256

        fun showInstallPointersDialog(context: Context) {
            MaterialDialog.Builder(context)
                    .title("Install Pointers")
                    .content("No Pointers installed. Please install some pointers")
                    .positiveText("Install Pointers")
                    .onPositive { _: MaterialDialog, _: DialogAction ->
                        TODO("Add Fragment")
                    }.show()
        }

        fun showInstallPointerFragment(activity: FragmentActivity) {
            activity.supportFragmentManager.beginTransaction().replace(R.id.fragment_container, InstallPointerFragment()).commit()
        }
    }
}*/

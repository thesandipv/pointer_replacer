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

package com.afterroot.allusive.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import com.afterroot.allusive.R
import com.afterroot.allusive.fragment.InstallPointerFragment
import com.afterroot.allusive.utils.DatabaseFields
import com.afterroot.allusive.utils.Helper
import com.afterroot.allusive.utils.PermissionChecker
import com.afterroot.allusive.utils.User
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_dashboard.*
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.longSnackbar

class DashboardActivity : AppCompatActivity() {

    private lateinit var toolbar: ActionBar
    val TAG = "DashboardActivity"
    private val showTouches = "show_touches"

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_Light)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        toolbar = supportActionBar!!
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }

    override fun onStart() {
        super.onStart()

        when {
            FirebaseAuth.getInstance().currentUser == null && Helper.isNetworkAvailable(this) ->
                startActivityForResult(AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(arrayListOf(AuthUI.IdpConfig.EmailBuilder().build(),
                                AuthUI.IdpConfig.GoogleBuilder().build()))
                        .setLogo(R.drawable.ic_launch_screen)
                        .build(), RC_LOGIN)
            !Helper.isNetworkAvailable(this) -> AlertDialog.Builder(this)
                    .setMessage("No Network Available")
                    .setTitle("No Network")
                    .setPositiveButton("RETRY") { _, _ -> onStart() }
                    .setNegativeButton("Cancel") { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            else -> initialize()
        }

    }

    private fun initialize() {
        //Firebase Analytics
        val bundle = Bundle()
        with(bundle) {
            putString("Device_Name", Build.DEVICE)
            putString("Manufacturer", Build.MANUFACTURER)
            putString("AndroidVersion", Build.VERSION.RELEASE)
        }
        FirebaseAnalytics.getInstance(this).logEvent("DeviceInfo", bundle)

        //Initialize Interstitial Ad
        MobileAds.initialize(this, getString(R.string.ad_interstitial_1_id))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions()
        } else {
            Log.d(TAG, "onStart: Loading fragments..")
            loadFragments()

            if (Settings.System.getInt(contentResolver, showTouches) == 0) {
                container.longSnackbar(getString(R.string.enable_touches_prompt), getString(R.string.prompt_button_enable)) {
                    Settings.System.putInt(contentResolver, showTouches, 1)
                }
            }
        }

        //Add user in db if not available
        val db = FirebaseFirestore.getInstance()
        FirebaseAuth.getInstance().currentUser.let {
            if (it != null) {
                db.collection(DatabaseFields.USERS)
                        .document(it.uid).set(User(it.displayName!!, it.email!!, it.uid))
                Toast.makeText(this, it.displayName, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RC_LOGIN -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Toast.makeText(this, "Welcome", Toast.LENGTH_SHORT).show()
                        initialize()
                    }
                    else -> {
                        Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private val manifestPermissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_SETTINGS)

    private fun checkPermissions() {
        Log.d(TAG, "checkPermissions: Checking Permissions..")
        val permissionChecker = PermissionChecker(this)
        if (permissionChecker.lacksPermissions(manifestPermissions)) {
            Log.d(TAG, "checkPermissions: Requesting Permissions..")
            ActivityCompat.requestPermissions(this, manifestPermissions, RC_PERMISSION)
        } else {
            Log.d(TAG, "checkPermissions: Permissions Granted..")
            loadFragments()

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    when {
                        Settings.System.canWrite(this) ->
                            when {
                                Settings.System.getInt(contentResolver, "show_touches") == 0 ->
                                    container.indefiniteSnackbar(getString(R.string.enable_touches_prompt), getString(R.string.prompt_button_enable)) {
                                        Settings.System.putInt(contentResolver,
                                                "show_touches", 1)
                                    }.show()
                            }
                        else -> {
                            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                            intent.data = Uri.parse("package:$packageName")
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
                    Log.d(TAG, "onRequestPermissionsResult: Permissions not Granted..")
                    Snackbar.make(this.container, "Please Grant Permissions", Snackbar.LENGTH_INDEFINITE).setAction("GRANT") {
                        checkPermissions()
                    }
                } else {
                    loadFragments()
                }
            }
        }
    }

    lateinit var appBarConfiguration: AppBarConfiguration
    private fun loadFragments() {
        val host: NavHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_repo_nav) as NavHostFragment?
                ?: return
        val navController = host.navController

        appBarConfiguration = AppBarConfiguration(navController.graph)

        setupActionBarWithNavController(navController, appBarConfiguration)
        navigation.setupWithNavController(navController)
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.home_dest -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.repo_dest -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.settings_dest -> {
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.profile_logout -> {
                AuthUI.getInstance().signOut(this).addOnCompleteListener { task ->

                }
            }
            else -> {
                return item.onNavDestinationSelected(findNavController(R.id.fragment_repo_nav)) || super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.fragment_repo_nav).navigateUp(appBarConfiguration)
    }

    companion object {
        const val RC_PERMISSION = 256
        const val RC_LOGIN = 42


        fun showInstallPointerFragment(activity: FragmentActivity) {
            activity.supportFragmentManager.beginTransaction().replace(R.id.container, InstallPointerFragment()).commit()
        }
    }
}

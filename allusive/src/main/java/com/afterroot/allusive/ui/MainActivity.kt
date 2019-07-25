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
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.afterroot.allusive.R
import com.afterroot.allusive.database.Database
import com.afterroot.allusive.database.DatabaseFields
import com.afterroot.allusive.model.User
import com.afterroot.allusive.ui.SplashActivity.Companion.RC_LOGIN
import com.afterroot.allusive.utils.PermissionChecker
import com.afterroot.allusive.utils.getPrefs
import com.afterroot.allusive.utils.isNetworkAvailable
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_dashboard.*
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var sharedPreferences: SharedPreferences
    private val _tag = this.javaClass.simpleName
    private val manifestPermissions =
        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_SETTINGS)
    private val showTouches = "show_touches"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        setSupportActionBar(toolbar)
        sharedPreferences = this.getPrefs()
    }

    override fun onStart() {
        super.onStart()
        if (FirebaseAuth.getInstance().currentUser == null && this.isNetworkAvailable()) {
            startActivityForResult(
                AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(
                        arrayListOf(
                            AuthUI.IdpConfig.EmailBuilder().build(),
                            AuthUI.IdpConfig.GoogleBuilder().build()
                        )
                    )
                    .setLogo(R.drawable.ic_launch_screen)
                    .build(), RC_LOGIN
            )
        } else if (!this.isNetworkAvailable()) {
            AlertDialog.Builder(this)
                .setMessage("No Network Available")
                .setTitle("No Network")
                .setPositiveButton("RETRY") { _, _ -> onStart() }
                .setNegativeButton("Cancel") { _, _ -> finish() }
                .setCancelable(false)
                .show()
        } else initialize()

    }

    private fun initialize() {
        if (sharedPreferences.getBoolean("first_install", true)) {
            Bundle().apply {
                putString("Device_Name", Build.DEVICE)
                putString("Manufacturer", Build.MANUFACTURER)
                putString("AndroidVersion", Build.VERSION.RELEASE)
                FirebaseAnalytics.getInstance(this@MainActivity).logEvent("DeviceInfo", this)
            }
            sharedPreferences.edit(true) { putBoolean("first_install", false) }
        }

        //Initialize Interstitial Ad
        MobileAds.initialize(this, getString(R.string.ad_interstitial_1_id))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions()
        } else {
            Log.d(_tag, "onStart: Loading fragments..")
            loadFragments()

            if (Settings.System.getInt(contentResolver, showTouches) == 0) {
                container.longSnackbar(
                    getString(R.string.enable_touches_prompt),
                    getString(R.string.prompt_button_enable)
                ) {
                    Settings.System.putInt(contentResolver, showTouches, 1)
                }
            }
        }

        //Add user in db if not available
        //addUserInfoInDB()
    }

    private fun addUserInfoInDB() {
        try {
            val auth = FirebaseAuth.getInstance()
            val curUser = auth.currentUser
            /*if (curUser?.displayName == null || curUser.email == null || curUser.phoneNumber == null) {
            createdView.findNavController().navigate(R.id.edit_profile)
            return
        }*/
            val userRef = Database.getDb().collection(DatabaseFields.USERS).document(curUser!!.uid)
            userRef.get().addOnCompleteListener { getUserTask ->
                when {
                    getUserTask.isSuccessful -> if (!getUserTask.result!!.exists()) {
                        container.snackbar("User not available. Creating User..")
                        val user = User(
                            curUser.displayName!!,
                            curUser.email!!,
                            curUser.uid
                        )
                        Log.d(_tag, "addUserInfoInDB: $user")
                        //TODO add dialog to add phone number
                        userRef.set(user).addOnCompleteListener { setUserTask ->
                            when {
                                setUserTask.isSuccessful -> {
                                }
                                else -> Log.e(
                                    _tag,
                                    "Can't create firebaseUser",
                                    setUserTask.exception
                                )
                            }
                        }
                    }
                    else -> Log.e(_tag, "Unknown Error", getUserTask.exception)
                }
                //initFirebaseDb()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkPermissions() {
        Log.d(_tag, "checkPermissions: Checking Permissions..")
        val permissionChecker = PermissionChecker(this)
        if (permissionChecker.lacksPermissions(manifestPermissions)) {
            Log.d(_tag, "checkPermissions: Requesting Permissions..")
            ActivityCompat.requestPermissions(this, manifestPermissions, RC_PERMISSION)
        } else {
            Log.d(_tag, "checkPermissions: Permissions Granted..")
            loadFragments()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                when (Settings.System.canWrite(this)) {
                    true -> {
                        if (Settings.System.getInt(contentResolver, "show_touches") == 0) {
                            container.indefiniteSnackbar(
                                getString(R.string.enable_touches_prompt),
                                getString(R.string.prompt_button_enable)
                            ) {
                                Settings.System.putInt(
                                    contentResolver,
                                    "show_touches", 1
                                )
                            }
                        }
                    }
                    false -> {
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        intent.data = Uri.parse("package:$packageName")
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            RC_PERMISSION -> {
                val isPermissionGranted =
                    grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (!isPermissionGranted) {
                    Log.d(_tag, "onRequestPermissionsResult: Permissions not Granted..")
                    Snackbar.make(
                        this.container,
                        "Please Grant Permissions",
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction("GRANT") {
                            checkPermissions()
                        }
                } else {
                    loadFragments()
                }
            }
        }
    }

    private fun loadFragments() {
        val host: NavHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_repo_nav) as NavHostFragment?
                ?: return
        val navController = host.navController
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            fab_apply.hide()
            when (destination.id) {
                R.id.mainFragment -> {
                    fab_apply.apply {
                        show()
                        text = getString(R.string.text_action_apply)
                    }
                }
                R.id.repoFragment -> {
                    fab_apply.apply {
                        show()
                        text = "POST"
                    }
                }
                R.id.settingsFragment -> {
                }
                R.id.editProfileFragment -> {
                    fab_apply.apply {
                        show()
                        text = "Save"
                    }
                }
            }

        }

        appBarConfiguration =
            AppBarConfiguration(setOf(R.id.mainFragment, R.id.repoFragment, R.id.settingsFragment))

        setupActionBarWithNavController(navController, appBarConfiguration)
        navigation.setupWithNavController(navController)
    }

    /*override fun onCreateOptionsMenu(menu: Menu?): Boolean {
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
    }*/

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.fragment_repo_nav).navigateUp(appBarConfiguration)
    }

    companion object {
        const val RC_PERMISSION = 256
    }
}

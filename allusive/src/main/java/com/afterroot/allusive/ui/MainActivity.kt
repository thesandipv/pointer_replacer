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
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.room.Room
import com.afterroot.allusive.BuildConfig
import com.afterroot.allusive.Constants.PREF_KEY_FIRST_INSTALL
import com.afterroot.allusive.Constants.RC_PERMISSION
import com.afterroot.allusive.R
import com.afterroot.allusive.database.DatabaseFields
import com.afterroot.allusive.database.MyDatabase
import com.afterroot.allusive.database.dbInstance
import com.afterroot.allusive.model.User
import com.afterroot.allusive.utils.*
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_dashboard.*
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.toast

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var sharedPreferences: SharedPreferences
    private val _tag = this.javaClass.simpleName
    private val manifestPermissions =
        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_SETTINGS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        setSupportActionBar(toolbar)
        sharedPreferences = this.getPrefs()
    }

    override fun onStart() {
        super.onStart()
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, SplashActivity::class.java))
        } else initialize()
    }

    private fun initialize() {
        if (sharedPreferences.getBoolean(PREF_KEY_FIRST_INSTALL, true)) {
            Bundle().apply {
                putString("Device_Name", Build.DEVICE)
                putString("Manufacturer", Build.MANUFACTURER)
                putString("AndroidVersion", Build.VERSION.RELEASE)
                FirebaseAnalytics.getInstance(this@MainActivity).logEvent("DeviceInfo", this)
            }
            sharedPreferences.edit(true) { putBoolean(PREF_KEY_FIRST_INSTALL, false) }
        }

        //Initialize AdMob SDK
        MobileAds.initialize(this, getString(R.string.admob_app_id))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions()
        } else {
            loadFragments()
        }

        if (BuildConfig.DEBUG) {
            val tag = "FCMToken"
            FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w(tag, "getInstanceId failed", task.exception)
                        return@OnCompleteListener
                    }

                    // Get new Instance ID token
                    val token = task.result?.token

                    // Log and toast
                    val msg = "InstanceID Token: $token"
                    Log.d(tag, msg)
                    baseContext.toast(msg)
                })
        }

        //Add user in db if not available
        //addUserInfoInDB()
    }

    //TODO Get to work addUserInfoInDB function
    private fun addUserInfoInDB() {
        try {
            val curUser = FirebaseUtils.auth!!.currentUser
            val userRef = dbInstance.collection(DatabaseFields.COLLECTION_USERS).document(curUser!!.uid)
            userRef.get().addOnCompleteListener { getUserTask ->
                when {
                    getUserTask.isSuccessful -> if (!getUserTask.result!!.exists()) {
                        container.snackbar("User not available. Creating User..").anchorView = navigation
                        val user = User(curUser.displayName, curUser.email, curUser.uid)
                        userRef.set(user).addOnCompleteListener { setUserTask ->
                            when {
                                setUserTask.isSuccessful -> {
                                }
                                else -> Log.e(_tag, "Can't create firebaseUser", setUserTask.exception)
                            }
                        }
                    }
                    else -> Log.e(_tag, "Unknown Error", getUserTask.exception)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkPermissions() {
        val permissionChecker = PermissionChecker(this)
        if (permissionChecker.lacksPermissions(manifestPermissions)) {
            ActivityCompat.requestPermissions(this, manifestPermissions, RC_PERMISSION)
        } else {
            loadFragments()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            RC_PERMISSION -> {
                val isPermissionGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (!isPermissionGranted) {
                    container.indefiniteSnackbar(
                        getString(R.string.msg_grant_app_permissions),
                        getString(R.string.text_action_grant)
                    ) {
                        checkPermissions()
                    }.anchorView = navigation
                } else {
                    loadFragments()
                }
            }
        }
    }

    private fun hideNavigation() {
        if (navigation.isVisible) {
            navigation.run {
                animateProperty("translationY", 0f, navigation.height.toFloat(), 200)
                visible(false)
            }
        }
    }

    private fun showNavigation() {
        if (!navigation.isVisible) {
            navigation.run {
                animateProperty("translationY", navigation.height.toFloat(), 0f, 200)
                visible(true)
            }
        }
    }

    private fun loadFragments() {
        val host: NavHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_repo_nav) as NavHostFragment? ?: return
        val navController = host.navController
        navController.addOnDestinationChangedListener { _, destination, _ ->
            //fab_apply.hide()
            showNavigation()
            when (destination.id) {
                R.id.mainFragment -> {
                    fab_apply.apply {
                        if (!isShown) show()
                        text = getString(R.string.text_action_apply)
                    }
                }
                R.id.repoFragment -> {
                    fab_apply.apply {
                        if (!isShown) show()
                        text = getString(R.string.text_action_post)
                    }
                }
                R.id.settingsFragment -> {
                    fab_apply.hide()
                }
                R.id.editProfileFragment -> {
                    fab_apply.apply {
                        if (!isShown) show()
                        text = getString(R.string.text_action_save)
                    }
                    hideNavigation()
                }
                R.id.newPostFragment -> {
                    fab_apply.apply {
                        if (!isShown) show()
                        text = getString(R.string.text_action_upload)
                    }
                    hideNavigation()
                }
                R.id.customizeFragment -> {
                    fab_apply.apply {
                        if (!isShown) show()
                        text = getString(R.string.text_action_apply)
                    }
                    hideNavigation()
                }
            }
        }

        appBarConfiguration = AppBarConfiguration(setOf(R.id.mainFragment, R.id.repoFragment, R.id.settingsFragment))

        setupActionBarWithNavController(navController, appBarConfiguration)
        navigation.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.fragment_repo_nav).navigateUp(appBarConfiguration)
    }

    companion object {
        private var myDatabase: MyDatabase? = null

        fun getDatabase(context: Context): MyDatabase {
            return myDatabase ?: Room.databaseBuilder(context, MyDatabase::class.java, "installed-pointers").build()
        }

    }
}

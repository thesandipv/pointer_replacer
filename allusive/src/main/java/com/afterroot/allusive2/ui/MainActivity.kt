/*
 * Copyright (C) 2016-2021 Sandip Vaghela
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

package com.afterroot.allusive2.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.allusive2.BuildConfig
import com.afterroot.allusive2.R
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.databinding.ActivityDashboardBinding
import com.afterroot.allusive2.model.User
import com.afterroot.allusive2.utils.FirebaseUtils
import com.afterroot.allusive2.utils.showNetworkDialog
import com.afterroot.allusive2.viewmodel.EventObserver
import com.afterroot.allusive2.viewmodel.MainSharedViewModel
import com.afterroot.allusive2.viewmodel.NetworkViewModel
import com.afterroot.core.extensions.animateProperty
import com.afterroot.core.extensions.visible
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import org.jetbrains.anko.design.snackbar
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var navigation: BottomNavigationView
    private lateinit var fabApply: ExtendedFloatingActionButton
    private val networkViewModel: NetworkViewModel by viewModel()
    private val settings: Settings by inject()
    private val sharedViewModel: MainSharedViewModel by viewModels()
    private val firebaseUtils: FirebaseUtils by inject()
    //private val manifestPermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_SETTINGS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dashboard)
        setSupportActionBar(binding.toolbar)
        title = null
    }

    override fun onStart() {
        super.onStart()
        if (get<FirebaseAuth>().currentUser == null) { //If not logged in, go to login.
            startActivity(Intent(this, SplashActivity::class.java))
        } else initialize()
    }

    private fun initialize() {
        if (settings.isFirstInstalled) {
            Bundle().apply {
                putString("Device_Name", Build.DEVICE)
                putString("Device_Model", Build.MODEL)
                putString("Manufacturer", Build.MANUFACTURER)
                putString("AndroidVersion", Build.VERSION.RELEASE)
                putString("AppVersion", BuildConfig.VERSION_CODE.toString())
                putString("Package", BuildConfig.APPLICATION_ID)
                FirebaseAnalytics.getInstance(this@MainActivity).logEvent("DeviceInfo2", this)
            }
            settings.isFirstInstalled = false
        }

        navigation = findViewById(R.id.navigation)
        fabApply = findViewById(R.id.fab_apply)

        //Initialize AdMob SDK
        MobileAds.initialize(this)

        //Add user in db if not available
        addUserInfoInDB()
        createPointerFolder()
        setUpNavigation()
        setUpTitleObserver()
        setUpErrorObserver()
        setUpNetworkObserver()

    }

    private fun setUpErrorObserver() {
        sharedViewModel.snackbarMsg.observe(this, EventObserver {
            findViewById<CoordinatorLayout>(R.id.container).snackbar(it).anchorView = navigation
        })
    }

    private fun setUpTitleObserver() {
        binding.titleBarTitleSwitcher.apply {
            setInAnimation(this@MainActivity, R.anim.text_switcher_in)
            setOutAnimation(this@MainActivity, R.anim.text_switcher_out)
        }
        sharedViewModel.liveTitle.observe(this, {
            updateTitle(it)
        })
    }

    private fun setTitle(title: String?) {
        sharedViewModel.setTitle(title)
    }

    private fun updateTitle(title: String?) {
        binding.apply {
            val params = fragmentRepoNav.layoutParams as CoordinatorLayout.LayoutParams
            if (title.isNullOrBlank()) {
                params.behavior = null
                titleLayout.visible(false)
            } else {
                params.behavior = AppBarLayout.ScrollingViewBehavior()
                //this.title = title
                titleLayout.visible(true)
                titleBarTitleSwitcher.setText(title)

            }
        }
    }

    private var dialog: MaterialDialog? = null
    private fun setUpNetworkObserver() {
        networkViewModel.monitor(this, onConnect = {
            if (dialog != null && dialog?.isShowing!!) dialog?.dismiss()
        }, onDisconnect = {
            dialog = showNetworkDialog(
                state = it,
                positive = { dialog?.dismiss() },
                negative = { finish() },
                isShowHide = true
            )
        })
    }

    private fun createPointerFolder() {
        val targetPath = "${filesDir.path}${getString(R.string.pointer_folder_path_new)}"
        val pointersFolder = File(targetPath)
        val dotNoMedia = File("${targetPath}/.nomedia")
        if (!pointersFolder.exists()) {
            pointersFolder.mkdirs()
        }
        if (!dotNoMedia.exists()) {
            dotNoMedia.createNewFile()
        }
    }

    private fun addUserInfoInDB() {
        try {
            val curUser = firebaseUtils.firebaseUser!!
            val userRef = get<FirebaseFirestore>().collection(DatabaseFields.COLLECTION_USERS).document(curUser.uid)
            get<FirebaseMessaging>().token
                .addOnCompleteListener(OnCompleteListener { tokenTask ->
                    if (!tokenTask.isSuccessful) {
                        return@OnCompleteListener
                    }
                    userRef.get().addOnCompleteListener { getUserTask ->
                        if (getUserTask.isSuccessful) {
                            if (!getUserTask.result!!.exists()) {
                                sharedViewModel.displayMsg("User not available. Creating User..")
                                val user = User(curUser.displayName, curUser.email, curUser.uid, tokenTask.result)
                                userRef.set(user).addOnCompleteListener { setUserTask ->
                                    if (!setUserTask.isSuccessful) Log.e(
                                        TAG,
                                        "Can't create firebaseUser",
                                        setUserTask.exception
                                    )
                                }
                            } else if (getUserTask.result!![DatabaseFields.FIELD_FCM_ID] != tokenTask.result) {
                                userRef.update(DatabaseFields.FIELD_FCM_ID, tokenTask.result)
                            }

                        } else Log.e(TAG, "Unknown Error", getUserTask.exception)
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "addUserInfoInDB: $e")
        }
    }

/*
    private fun checkPermissions() {
        val permissionChecker = PermissionChecker(this)
        if (permissionChecker.lacksPermissions(manifestPermissions)) {
            ActivityCompat.requestPermissions(this, manifestPermissions, RC_PERMISSION)
        } else {
            createPointerFolder()
        }
    }
*/

/*
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
*/

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

    private fun setUpNavigation() {
        val host: NavHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_repo_nav) as NavHostFragment? ?: return
        val navController = host.navController
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (!fabApply.isExtended) {
                fabApply.extend()
            }
            showNavigation()
            when (destination.id) {
                R.id.mainFragment -> {
                    fabApply.apply {
                        if (!isShown) show()
                        text = getString(R.string.text_action_apply)
                    }
                    setTitle(getString(R.string.title_home))
                }
                R.id.repoFragment -> {
                    fabApply.apply {
                        if (!isShown) show()
                        text = getString(R.string.text_action_post)
                    }
                    setTitle(getString(R.string.title_dashboard))
                }
                R.id.settingsFragment -> {
                    fabApply.hide()
                    setTitle(getString(R.string.title_settings))
                }
                R.id.editProfileFragment -> {
                    fabApply.apply {
                        if (!isShown) show()
                        text = getString(R.string.text_action_save)
                    }
                    hideNavigation()
                    setTitle(getString(R.string.title_edit_profile))
                }
                R.id.newPostFragment -> {
                    fabApply.apply {
                        if (!isShown) show()
                        text = getString(R.string.text_action_upload)
                    }
                    hideNavigation()
                    setTitle(getString(R.string.title_new_pointer))
                }
                R.id.customizeFragment -> {
                    fabApply.apply {
                        if (!isShown) show()
                        text = getString(R.string.text_action_apply)
                    }
                    hideNavigation()
                    setTitle(getString(R.string.title_customizer_pointer))
                }
                R.id.magiskFragment -> {
                    fabApply.hide()
                    hideNavigation()
                    setTitle("Apply with Magisk")
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
        private const val TAG = "MainActivity"
    }
}

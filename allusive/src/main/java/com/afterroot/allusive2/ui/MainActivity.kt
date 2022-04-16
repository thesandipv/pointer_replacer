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

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
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
import com.afterroot.allusive2.BuildConfig
import com.afterroot.allusive2.R
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.databinding.ActivityDashboardBinding
import com.afterroot.allusive2.model.User
import com.afterroot.allusive2.utils.showNetworkDialog
import com.afterroot.allusive2.viewmodel.EventObserver
import com.afterroot.allusive2.viewmodel.MainSharedViewModel
import com.afterroot.allusive2.viewmodel.NetworkViewModel
import com.afterroot.core.extensions.animateProperty
import com.afterroot.core.extensions.visible
import com.afterroot.data.utils.FirebaseUtils
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import org.jetbrains.anko.design.snackbar
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import com.afterroot.allusive2.resources.R as CommonR

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var navigation: BottomNavigationView
    private lateinit var fabApply: ExtendedFloatingActionButton
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: MainSharedViewModel by viewModels()
    @Inject lateinit var settings: Settings
    @Inject lateinit var firebaseUtils: FirebaseUtils
    @Inject lateinit var firebaseAnalytics: FirebaseAnalytics
    @Inject lateinit var firestore: FirebaseFirestore
    @Inject lateinit var firebaseMessaging: FirebaseMessaging
    // private val manifestPermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_SETTINGS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dashboard)
        setSupportActionBar(binding.toolbar)
        title = null
    }

    override fun onStart() {
        super.onStart()
        if (!firebaseUtils.isUserSignedIn) { // If not logged in, go to login.
            startActivity(Intent(this, SplashActivity::class.java))
        } else initialize()
    }

    @SuppressLint("MissingPermission")
    private fun initialize() {
        if (settings.isFirstInstalled) {
            Bundle().apply {
                putString("Device_Name", Build.DEVICE)
                putString("Device_Model", Build.MODEL)
                putString("Manufacturer", Build.MANUFACTURER)
                putString("AndroidVersion", Build.VERSION.RELEASE)
                putString("AppVersion", BuildConfig.VERSION_CODE.toString())
                putString("Package", BuildConfig.APPLICATION_ID)
                firebaseAnalytics.logEvent("DeviceInfo2", this)
            }
            settings.isFirstInstalled = false
        }

        navigation = findViewById(R.id.navigation)
        fabApply = findViewById(R.id.fab_apply)

        // Initialize AdMob SDK
        MobileAds.initialize(this)

        // Add user in db if not available
        addUserInfoInDB()
        createPointerFolder()
        setUpNavigation()
        setUpTitleObserver()
        setUpErrorObserver()
        setUpNetworkObserver()
    }

    private fun setUpErrorObserver() {
        sharedViewModel.snackbarMsg.observe(
            this,
            EventObserver {
                findViewById<CoordinatorLayout>(R.id.container).snackbar(it).anchorView = navigation
            }
        )
    }

    private fun setUpTitleObserver() {
        binding.titleBarTitleSwitcher.apply {
            setInAnimation(this@MainActivity, CommonR.anim.text_switcher_in)
            setOutAnimation(this@MainActivity, CommonR.anim.text_switcher_out)
        }
        sharedViewModel.liveTitle.observe(this) {
            updateTitle(it)
        }
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
                // this.title = title
                titleLayout.visible(true)
                titleBarTitleSwitcher.setText(title)
            }
        }
    }

    private var dialog: AlertDialog? = null
    private fun setUpNetworkObserver() {
        networkViewModel.monitor(
            this,
            onConnect = {
                if (dialog != null && dialog?.isShowing!!) dialog?.dismiss()
            },
            onDisconnect = {
                dialog = showNetworkDialog(
                    state = it,
                    positive = { dialog?.dismiss() },
                    negative = { finish() },
                    isShowHide = true
                )
            }
        )
    }

    private fun createPointerFolder() {
        val targetPath = "${filesDir.path}${getString(CommonR.string.pointer_folder_path_new)}"
        val pointersFolder = File(targetPath)
        val dotNoMedia = File("$targetPath/.nomedia")
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
            val userRef = firestore.collection(DatabaseFields.COLLECTION_USERS).document(curUser.uid)
            firebaseMessaging.token
                .addOnCompleteListener(
                    OnCompleteListener { tokenTask ->
                        if (!tokenTask.isSuccessful) {
                            return@OnCompleteListener
                        }
                        userRef.get().addOnCompleteListener { getUserTask ->
                            if (getUserTask.isSuccessful) {
                                if (!getUserTask.result!!.exists()) {
                                    sharedViewModel.displayMsg("User not available. Creating User..")
                                    val user = User(curUser.displayName, curUser.email, curUser.uid, tokenTask.result)
                                    userRef.set(user).addOnCompleteListener { setUserTask ->
                                        if (!setUserTask.isSuccessful) Timber.tag(TAG)
                                            .e(setUserTask.exception, "Can't create firebaseUser")
                                    }
                                } else if (getUserTask.result!![DatabaseFields.FIELD_FCM_ID] != tokenTask.result) {
                                    userRef.update(DatabaseFields.FIELD_FCM_ID, tokenTask.result)
                                }
                            } else Timber.tag(TAG).e(getUserTask.exception, "Unknown Error")
                        }
                    }
                )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "addUserInfoInDB")
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
                        text = getString(CommonR.string.text_action_apply)
                    }
                    setTitle(getString(CommonR.string.title_home))
                }
                R.id.repoFragment -> {
                    fabApply.apply {
                        if (!isShown) show()
                        text = getString(CommonR.string.text_action_post)
                    }
                    setTitle(getString(CommonR.string.title_dashboard))
                }
                R.id.settingsFragment -> {
                    fabApply.hide()
                    setTitle(getString(CommonR.string.title_settings))
                }
                R.id.editProfileFragment -> {
                    fabApply.apply {
                        if (!isShown) show()
                        text = getString(CommonR.string.text_action_save)
                    }
                    hideNavigation()
                    setTitle(getString(CommonR.string.title_edit_profile))
                }
                R.id.newPostFragment -> {
                    fabApply.apply {
                        if (!isShown) show()
                        text = getString(CommonR.string.text_action_upload)
                    }
                    hideNavigation()
                    setTitle(getString(CommonR.string.title_new_pointer))
                }
                R.id.customizeFragment -> {
                    fabApply.apply {
                        if (!isShown) show()
                        text = getString(CommonR.string.text_action_apply)
                    }
                    hideNavigation()
                    setTitle(getString(CommonR.string.title_customizer_pointer))
                }
                R.id.magiskFragment -> {
                    fabApply.hide()
                    hideNavigation()
                    setTitle("Apply with Magisk")
                }
                R.id.magiskRROFragment -> {
                    fabApply.hide()
                    hideNavigation()
                    setTitle("Apply with Magisk [RRO]")
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

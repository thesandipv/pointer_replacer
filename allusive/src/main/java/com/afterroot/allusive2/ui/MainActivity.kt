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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.afterroot.allusive2.BuildConfig
import com.afterroot.allusive2.R
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.data.mapper.toNetworkUser
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.databinding.ActivityDashboardBinding
import com.afterroot.allusive2.home.HomeActions
import com.afterroot.allusive2.utils.addMenuProviderExt
import com.afterroot.allusive2.utils.showNetworkDialog
import com.afterroot.allusive2.utils.whenBuildIs
import com.afterroot.allusive2.viewmodel.EventObserver
import com.afterroot.allusive2.viewmodel.MainSharedViewModel
import com.afterroot.allusive2.viewmodel.NetworkViewModel
import com.afterroot.data.model.NetworkUser
import com.afterroot.data.model.UserProperties
import com.afterroot.data.utils.FirebaseUtils
import com.afterroot.utils.VersionCheck
import com.afterroot.utils.data.model.VersionInfo
import com.afterroot.utils.extensions.animateProperty
import com.afterroot.utils.extensions.visible
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.email
import timber.log.Timber
import com.afterroot.allusive2.resources.R as CommonR

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var fabApply: ExtendedFloatingActionButton
    private lateinit var navigation: BottomNavigationView
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: MainSharedViewModel by viewModels()
    private var interstitialAd: InterstitialAd? = null

    @Inject @Named("feedback_body") lateinit var feedbackBody: String

    @Inject lateinit var firebaseAnalytics: FirebaseAnalytics

    @Inject lateinit var firebaseMessaging: FirebaseMessaging

    @Inject lateinit var firebaseUtils: FirebaseUtils

    @Inject lateinit var firestore: FirebaseFirestore

    @Inject lateinit var gson: Gson

    @Inject lateinit var remoteConfig: FirebaseRemoteConfig

    @Inject lateinit var settings: Settings
    // private val manifestPermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_SETTINGS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dashboard)
        setSupportActionBar(binding.toolbar)
        title = null

        addMenuProviderExt(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(CommonR.menu.menu_common, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    CommonR.id.send_feedback -> {
                        email(
                            email = "afterhasroot@gmail.com",
                            subject = "Pointer Replacer Feedback",
                            text = feedbackBody,
                        )
                        true
                    }
                    else -> menuItem.onNavDestinationSelected(
                        findNavController(R.id.fragment_repo_nav),
                    )
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        if (!firebaseUtils.isUserSignedIn) { // If not logged in, go to login.
            startActivity(Intent(this, SplashActivity::class.java))
        } else {
            initialize()
        }
    }

    override fun onResume() {
        super.onResume()
        setUpVersionCheck()
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
        loadInterstitialAd()
        lifecycleScope.launch { setUpActions() }
    }

    private suspend fun setUpActions() {
        sharedViewModel.actions.collect { action ->
            Timber.d("setUpActions: Collected Action: $action")
            when (action) {
                is HomeActions.LoadIntAd -> {
                    loadInterstitialAd(action.isShow)
                }
                HomeActions.ShowIntAd -> {
                    if (interstitialAd == null) {
                        loadInterstitialAd(true)
                    } else {
                        showInterstitialAd()
                    }
                }
                else -> {}
            }
        }
    }

    private fun loadInterstitialAd(isShow: Boolean = false) {
        if (interstitialAd != null) {
            return
        }
        val interstitialAdUnitId: String = whenBuildIs(
            debug = getString(CommonR.string.ad_interstitial_1_id),
            release = remoteConfig.getString("ad_interstitial_1_id"),
        )

        InterstitialAd.load(
            this,
            interstitialAdUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    if (isShow) showInterstitialAd()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    sharedViewModel.submitAction(HomeActions.OnIntAdDismiss)
                    super.onAdFailedToLoad(loadAdError)
                }
            },
        )
    }

    private fun showInterstitialAd(onAdDismiss: () -> Unit = {}) {
        interstitialAd?.let {
            it.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd()
                    sharedViewModel.submitAction(HomeActions.OnIntAdDismiss)
                    onAdDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    interstitialAd = null
                    sharedViewModel.submitAction(HomeActions.OnIntAdDismiss)
                    onAdDismiss()
                }
            }
            it.show(this)
        }
    }

    private fun setUpErrorObserver() {
        sharedViewModel.snackbarMsg.observe(
            this,
            EventObserver {
                findViewById<CoordinatorLayout>(R.id.container).snackbar(it).anchorView = navigation
            },
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
                    isShowHide = true,
                )
            },
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
            val userRef = firestore.collection(
                DatabaseFields.COLLECTION_USERS,
            ).document(curUser.uid)
            firebaseMessaging.token
                .addOnCompleteListener(
                    OnCompleteListener { tokenTask ->
                        if (!tokenTask.isSuccessful) {
                            return@OnCompleteListener
                        }
                        firebaseUtils.fcmId = tokenTask.result
                        userRef.get().addOnCompleteListener { getUserTask ->
                            if (getUserTask.isSuccessful) {
                                if (!getUserTask.result!!.exists()) {
                                    sharedViewModel.displayMsg(
                                        "User not available. Creating User..",
                                    )
                                    userRef.set(
                                        NetworkUser(
                                            name = curUser.displayName,
                                            email = curUser.email,
                                            uid = curUser.uid,
                                            fcmId = tokenTask.result,
                                        ),
                                    ).addOnCompleteListener { setUserTask ->
                                        if (!setUserTask.isSuccessful) {
                                            Timber.e(
                                                setUserTask.exception,
                                                "addUserInfoInDB: Can't create firebaseUser",
                                            )
                                        }
                                    }
                                } else if (getUserTask.result!![DatabaseFields.FIELD_FCM_ID] != tokenTask.result) {
                                    userRef.update(DatabaseFields.FIELD_FCM_ID, tokenTask.result)
                                } else if (getUserTask.result!![DatabaseFields.FIELD_VERSION] == null) {
                                    Timber.d("addUserInfoInDB: Migrating to v1")
                                    userRef.update(
                                        hashMapOf(
                                            DatabaseFields.FIELD_VERSION to 1,
                                            DatabaseFields.FIELD_USERNAME to null,
                                            DatabaseFields.FIELD_USER_PROPERTIES to UserProperties(),
                                        ),
                                    )
                                } // Add Future Migrations Here
                                userRef.get(Source.CACHE).addOnSuccessListener {
                                    firebaseUtils.networkUser = it.toNetworkUser()
                                }
                            } else {
                                Timber.e(
                                    getUserTask.exception,
                                    "addUserInfoInDB: ${getUserTask.exception?.message}",
                                )
                            }
                        }
                    },
                )
        } catch (e: Exception) {
            Timber.e(e, "addUserInfoInDB: ${e.message}")
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
                R.id.rroRequestFragment -> {
                    fabApply.hide()
                    hideNavigation()
                    setTitle(getString(CommonR.string.text_your_requests))
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

    private fun setUpVersionCheck() {
        sharedViewModel.savedStateHandle.getLiveData<Boolean>(MainSharedViewModel.KEY_CONFIG_LOADED)
            .observe(this) {
                if (!it) return@observe
                val versionJson = remoteConfig.getString("versions_allusive")
                if (versionJson.isBlank()) return@observe
                val versionChecker = VersionCheck(
                    gson.fromJson(versionJson, VersionInfo::class.java)
                        .copy(currentVersion = BuildConfig.VERSION_CODE),
                )
                versionChecker.onVersionDisabled {
                    AlertDialog.Builder(this).apply {
                        setTitle("Version Obsolete")
                        setMessage(
                            "This version is obsolete. You have to update to latest version.",
                        )
                        setPositiveButton("Update") { _, _ ->
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}",
                                ),
                            )
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        setNegativeButton(android.R.string.cancel) { _, _ ->
                            finish()
                        }
                        setCancelable(false)
                    }.show()
                }
                versionChecker.onUpdateAvailable {
                    AlertDialog.Builder(this).apply {
                        setTitle("Update Available")
                        setMessage("New Version Available. Please update to get latest features.")
                        setPositiveButton("Update") { _, _ ->
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}",
                                ),
                            )
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        setNegativeButton(android.R.string.cancel) { _, _ ->
                        }
                    }.show()
                }
            }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

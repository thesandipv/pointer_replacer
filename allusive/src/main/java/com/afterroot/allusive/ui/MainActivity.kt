/*
 * Copyright (C) 2016-2020 Sandip Vaghela
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
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.allusive.BuildConfig
import com.afterroot.allusive.Constants.RC_PERMISSION
import com.afterroot.allusive.Constants.RC_STORAGE_ACCESS
import com.afterroot.allusive.R
import com.afterroot.allusive.Settings
import com.afterroot.allusive.database.DatabaseFields
import com.afterroot.allusive.model.User
import com.afterroot.allusive.utils.FirebaseUtils
import com.afterroot.allusive.utils.PermissionChecker
import com.afterroot.core.extensions.animateProperty
import com.afterroot.core.extensions.visible
import com.afterroot.core.onVersionGreaterThanEqualTo
import com.afterroot.core.onVersionLessThan
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_dashboard.*
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.snackbar
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private val settings: Settings by inject()
    private val _tag = this.javaClass.simpleName
    private val manifestPermissions =
        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_SETTINGS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        setSupportActionBar(toolbar)
    }

    override fun onStart() {
        super.onStart()
        if (FirebaseAuth.getInstance().currentUser == null) { //If not logged in, go to login.
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

        //Initialize AdMob SDK
        MobileAds.initialize(this, getString(R.string.admob_app_id))

        onVersionGreaterThanEqualTo(Build.VERSION_CODES.LOLLIPOP, {
            //Greater than Lollipop
            when {
                settings.safUri == null -> { //SAF not Accessed
                    openStorageAccess(this)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    checkPermissions()
                }
                else -> {
                    createPointerFolder()
                }
            }
        }, {
            loadFragments() //Less than Lollipop, direct load fragments
        })

        //Add user in db if not available
        addUserInfoInDB()
    }

    private fun createPointerFolder() {
        onVersionLessThan(Build.VERSION_CODES.LOLLIPOP, {
            val targetPath = "${Environment.getExternalStorageDirectory()}${getString(R.string.pointer_folder_path)}"
            val pointersFolder = File(targetPath)
            val dotNoMedia = File("${targetPath}/.nomedia")
            if (!pointersFolder.exists()) {
                pointersFolder.mkdirs()
            }
            if (!dotNoMedia.exists()) {
                dotNoMedia.createNewFile()
            }
        }, {
            val pointerFolderName = "Pointer Replacer"
            val documentTree = DocumentFile.fromTreeUri(
                this,
                settings.safUri?.toUri()!!
            ) ?: return@onVersionLessThan
            if (documentTree.findFile(pointerFolderName) == null) {
                documentTree.createDirectory(pointerFolderName)
                    ?.createDirectory("Pointers")
                    ?.createFile("", ".nomedia")
            }
        })
        loadFragments()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_STORAGE_ACCESS && resultCode == Activity.RESULT_OK) {
            data?.data?.also {
                val uri = data.data ?: return
                settings.safUri = uri.toString()
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                onVersionGreaterThanEqualTo(Build.VERSION_CODES.KITKAT, {
                    this.applicationContext.contentResolver.takePersistableUriPermission(uri, flags)
                })
                onVersionGreaterThanEqualTo(Build.VERSION_CODES.M, {
                    checkPermissions() //Check for permissions after Storage access if Android M up
                }, {
                    createPointerFolder() //Else direct create folder if not exists if Lollipop
                })

            }
        }
    }

    private fun addUserInfoInDB() {
        try {
            val curUser = FirebaseUtils.auth!!.currentUser
            val userRef = get<FirebaseFirestore>().collection(DatabaseFields.COLLECTION_USERS).document(curUser!!.uid)
            FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener(OnCompleteListener { tokenTask ->
                    if (!tokenTask.isSuccessful) {
                        return@OnCompleteListener
                    }
                    userRef.get().addOnCompleteListener { getUserTask ->
                        if (getUserTask.isSuccessful) {
                            if (!getUserTask.result!!.exists()) {
                                container.snackbar("User not available. Creating User..").anchorView = navigation
                                val user = User(curUser.displayName, curUser.email, curUser.uid, tokenTask.result?.token!!)
                                userRef.set(user).addOnCompleteListener { setUserTask ->
                                    if (!setUserTask.isSuccessful) Log.e(
                                        _tag,
                                        "Can't create firebaseUser",
                                        setUserTask.exception
                                    )
                                }
                            } else if (getUserTask.result!![DatabaseFields.FIELD_FCM_ID] != tokenTask.result?.token!!) {
                                userRef.update(DatabaseFields.FIELD_FCM_ID, tokenTask.result?.token!!)
                            }

                        } else Log.e(_tag, "Unknown Error", getUserTask.exception)
                    }
                })
        } catch (e: Exception) {
            Log.e(_tag, "addUserInfoInDB: $e")
        }
    }

    private fun checkPermissions() {
        val permissionChecker = PermissionChecker(this)
        if (permissionChecker.lacksPermissions(manifestPermissions)) {
            ActivityCompat.requestPermissions(this, manifestPermissions, RC_PERMISSION)
        } else {
            createPointerFolder()
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
            if (!fab_apply.isExtended) {
                fab_apply.extend()
            }
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
        private const val TAG = "MainActivity"
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        fun openStorageAccess(activity: Activity) {
            MaterialDialog(activity).show {
                title(text = "Grant Storage Permissions")
                message(text = "On next screen Select Internal Storage > Tap Allow Access and Grant Storage permission.")
                positiveButton(text = "Grant") {
                    activity.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    }, RC_STORAGE_ACCESS)
                }
                negativeButton(res = android.R.string.cancel) {
                    activity.finish()
                }
                cancelable(false)
                cancelOnTouchOutside(false)
            }
        }

    }
}

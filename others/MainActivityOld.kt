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

package com.afterroot.allusive

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.transition.TransitionInflater
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.graphics.drawable.DrawerArrowDrawable
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.afollestad.materialdialogs.color.ColorChooserDialog
import com.afollestad.materialdialogs.folderselector.FileChooserDialog
import com.afterroot.allusive.Helper.hideView
import com.afterroot.allusive.Helper.showView
import com.afterroot.allusive.fragment.CustomizeFragment
import com.afterroot.allusive.fragment.MainFragment
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_main.*
import org.apache.commons.io.FileUtils
import sheetrock.panda.changelog.ChangeLog
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class MainActivityOld : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        ColorChooserDialog.ColorCallback, FileChooserDialog.FileCallback {
    private var mSharedPreferences: SharedPreferences? = null
    private var mEditor: SharedPreferences.Editor? = null
    private var mExtSdDir: String? = null
    private var mTargetPath: String? = null
    private var mPointerPreviewPath: String? = null
    private var mFolderPointers: File? = null
    private var mMainFragment: MainFragment? = null
    private var mCustomizeFragment: CustomizeFragment? = null
    private val manifestPermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_Light)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        //Firebase Analytics
        val analytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle().apply {
            putString("Device_Name", Build.DEVICE)
            putString("AndroidVersion", Build.VERSION.RELEASE)
        }
        analytics.logEvent("DeviceInfo", bundle)

        //Firebase Ads
        MobileAds.initialize(this, getString(R.string.interstitial_ad_1_id))
        nav_view.setNavigationItemSelectedListener(this)

        toolbar.navigationIcon = DrawerArrowDrawable(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            nav_view.itemIconTintList = resources.getColorStateList(R.color.nav_state_list, theme)
        } else {
            nav_view.itemIconTintList = resources.getColorStateList(R.color.nav_state_list)
            mMainFragment = MainFragment()
            mCustomizeFragment = CustomizeFragment()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val changeTransform = TransitionInflater.from(this).inflateTransition(R.transition.change_pointer_transform)
                val explode = TransitionInflater.from(this).inflateTransition(R.transition.explode)

                mCustomizeFragment!!.apply {
                    sharedElementEnterTransition = changeTransform
                    enterTransition = explode
                    exitTransition = explode
                    sharedElementReturnTransition = changeTransform
                }
            }
            setFragment(mMainFragment!!, false)
        }
        fab_tune.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                supportFragmentManager.beginTransaction()
                        .addSharedElement(selected_pointer, ViewCompat.getTransitionName(selected_pointer))
                        .replace(R.id.fragment_container, mCustomizeFragment)
                        .commitNow()
            } else {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, mCustomizeFragment)
                        .commitNow()
            }
        }

        initialize()
    }

    private fun setFragment(fragment: Fragment) {
        this.setFragment(fragment, false)
    }

    private fun setFragment(fragment: Fragment, isAddToBackStack: Boolean) {
        supportFragmentManager.apply {
            if (isAddToBackStack) {
                beginTransaction().replace(R.id.fragment_container, fragment).addToBackStack(fragment::class.java.simpleName).commit()
            } else {
                beginTransaction().replace(R.id.fragment_container, fragment).commit()
            }
        }
    }

    private var mInterstitialAd: InterstitialAd? = null
    @SuppressLint("CommitPrefEdits")
    private fun initialize() {
        /*Load SharedPreferences**/
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        mEditor = mSharedPreferences!!.edit()

        val pointersFolder = getString(R.string.pointer_folder_path)
        mExtSdDir = Environment.getExternalStorageDirectory().toString()
        mTargetPath = mExtSdDir!! + pointersFolder
        mPointerPreviewPath = filesDir.path + "/pointerPreview.png"

        loadMethods()

        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd!!.apply {
            adUnitId = getString(R.string.interstitial_ad_1_id)
            loadAd(AdRequest.Builder().build())
            adListener = object : AdListener() {
                override fun onAdClosed() {
                    mInterstitialAd!!.loadAd(AdRequest.Builder().build())
                }
            }
        }
    }

    /**
     * Load Methods
     */
    private fun loadMethods() {
        showChangelog()
        createPointersFolder()
    }

    private fun showPreview() {
        try {
            val bitmap = loadBitmapFromView(selected_pointer)
            val file = File(mPointerPreviewPath!!)
            Runtime.getRuntime().exec("chmod 666 " + mPointerPreviewPath!!)
            val out: FileOutputStream
            out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        startActivity(Intent(this, PointerPreview::class.java))
    }

    /**
     * Show A Changelog Dialog
     */
    private fun showChangelog() {
        val cl = ChangeLog(this)
        if (cl.firstRun()) cl.logDialog.show()
    }

    /**
     * Create Pointers Folder
     */
    private fun createPointersFolder() {
        mFolderPointers = File(mTargetPath!!)
        try {
            if (!mFolderPointers!!.exists() && mFolderPointers!!.mkdirs()) {

            }
            if (mFolderPointers!!.listFiles().isEmpty()) {

            }

            val dotNoMedia = File(mExtSdDir + File.separator + getString(R.string.app_name) + File.separator + ".nomedia")
            if (!dotNoMedia.exists()) {
                dotNoMedia.createNewFile()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**Show color picker dialog. */
    private fun showColorPicker() {
        AmbilWarnaDialog(this,
                mSharedPreferences!!.getInt(getString(R.string.key_oldColor), -1),
                object : AmbilWarnaDialog.OnAmbilWarnaListener {
                    override fun onCancel(dialog: AmbilWarnaDialog) {
                        Helper.showSnackBar(fragment_container, getString(R.string.text_color_not_changed))
                    }

                    override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                        selected_pointer.setColorFilter(color)
                        Helper.showSnackBar(fragment_container, getString(R.string.text_color_changed))
                        mEditor!!.putInt(getString(R.string.key_oldColor), color).apply()
                    }
                }).show()
    }

    /**
     * @throws IOException exception
     */
    @Throws(IOException::class)
    private fun applyPointer() {
        val pointerPath = filesDir.path + "/pointer.png"
        mEditor!!.putString(getString(R.string.key_pointerPath), pointerPath).apply()
        val bitmap = loadBitmapFromView(selected_pointer)
        val file = File(pointerPath)
        Runtime.getRuntime().exec("chmod 666 " + pointerPath)
        val out: FileOutputStream
        try {
            out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
            showView(current_pointer)
            hideView(text_no_pointer_applied)
            current_pointer.setImageDrawable(Drawable.createFromPath(pointerPath))
            showRebootDialog()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

    }

    /**Show a reboot confirm dialog */
    private fun showRebootDialog() {
        /*MaterialDialog.Builder(this)
                .title(R.string.reboot)
                .theme(Theme.DARK)
                .content(R.string.text_reboot_confirm)
                .positiveText(R.string.reboot)
                .negativeText(R.string.text_no)
                .neutralText(R.string.text_soft_reboot)
                .onPositive { _, _ ->
                    try {
                        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot"))
                        process.waitFor()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .onNeutral { _, _ ->
                    try {
                        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "busybox killall system_server"))
                        process.waitFor()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .show()*/

        AlertDialog.Builder(this, R.style.Theme_AppCompat_DayNight_Dialog_Alert)
                .setTitle(R.string.reboot)
                .setMessage(R.string.text_reboot_confirm)
                .setPositiveButton(R.string.reboot, { _, _ ->
                    try {
                        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot"))
                        process.waitFor()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                })
                .setNegativeButton(R.string.text_no, { _, _ ->

                })
                .setNeutralButton(R.string.text_soft_reboot, { _, _ ->
                    try {
                        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "busybox killall system_server"))
                        process.waitFor()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }).show()
    }

    private fun loadBitmapFromView(v: View?): Bitmap {
        val w = v!!.width
        val h = v.height
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        v.layout(v.left, v.top, v.right, v.bottom)
        v.draw(c)
        return b
    }

    override fun onColorSelection(dialog: ColorChooserDialog, selectedColor: Int) {
        selected_pointer.setColorFilter(selectedColor)
        Helper.showSnackBar(fragment_container, getString(R.string.text_color_changed))
        mEditor!!.putInt(getString(R.string.key_oldColor), selectedColor)

        mEditor!!.apply()
    }

    override fun onColorChooserDismissed(dialog: ColorChooserDialog) {

    }

    private val oldColor: Int
        get() = mSharedPreferences!!.getInt(getString(R.string.key_oldColor), -1)

    private fun showPointerColorChooser() {
        ColorChooserDialog.Builder(this, R.string.choose_pointer_color)
                .titleSub(R.string.choose_pointer_color)
                .accentMode(false)
                .allowUserColorInputAlpha(false)
                .dynamicButtonColor(false)
                .preselect(oldColor)
                .show(this)
    }

    private fun showSureDialog() {
        val drawable = selected_pointer.drawable
        MaterialDialog.Builder(this)
                .title(R.string.apply_pointer)
                .theme(Theme.DARK)
                .content(R.string.text_apply_pointer_confirm)
                .positiveText(R.string.text_yes)
                .negativeText(R.string.text_no)
                .neutralText(R.string.title_activity_pointer_preview)
                .onNeutral { _, _ -> showPreview() }
                .maxIconSize(50)
                .icon(drawable)
                .onPositive { _, _ ->
                    try {
                        applyPointer()
                        if (mInterstitialAd!!.isLoaded) {
                            mInterstitialAd!!.show()
                        } else {
                            Log.d(MainActivity::class.java.simpleName, "The interstitial wasn't loaded yet.")
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    Helper.showSnackBar(fragment_container, getString(R.string.text_pointer_applied))
                }.show()
    }

    override fun onFileSelection(dialog: FileChooserDialog, file: File) {
        Helper.showSnackBar(fragment_container, getString(R.string.text_selected_pointer) + ": " + file.name)
        selected_pointer.setImageDrawable(Drawable.createFromPath(file.absolutePath))

        if (File(mTargetPath!! + file.name).exists()) {
            Helper.showSnackBar(fragment_container, getString(R.string.text_pointer_exists))
        } else {
            try {
                FileUtils.copyFile(file, File(mTargetPath!! + file.name))
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    override fun onFileChooserDismissed(dialog: FileChooserDialog) {
    }

    override fun onBackPressed() {
        if (drawer_layout != null) {
            if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
                drawer_layout.closeDrawer(GravityCompat.START)
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_customize, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.reset_grid -> {
                run {
                    //CustomizeFragment.setPointerSizeBarProgress(mCustomizeFragment!!, minSize)
                    //seekBarPadding.progress = 0
                }
                true
            }
            R.id.viewPreview -> {
                showPreview()
                super.onOptionsItemSelected(item)
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_changelog -> {
                val cl = ChangeLog(this)
                cl.fullLogDialog.show()
            }
            R.id.change_color -> if (mSharedPreferences!!.getBoolean(getString(R.string.key_useMDCC), true)) {
                showPointerColorChooser()
            } else {
                showColorPicker()
            }
            R.id.apply_pointer -> showSureDialog()
            R.id.reboot -> showRebootDialog()
            R.id.reset_color -> {
                selected_pointer.colorFilter = null
                Helper.showSnackBar(fragment_container, getString(R.string.text_color_changed_default))
            }
            R.id.settings -> {
            }
            R.id.preview -> showPreview()
            R.id.about -> startActivity(Intent(this, AboutActivity::class.java))
            R.id.import_pointer -> FileChooserDialog.Builder(this)
                    .mimeType("image/*")
                    .show(this)
            R.id.manage_pointers -> startActivity(Intent(this, ManagePointerActivity::class.java))
        }

        if (drawer_layout != null) {
            drawer_layout.closeDrawer(GravityCompat.START)
        }
        return true
    }
}
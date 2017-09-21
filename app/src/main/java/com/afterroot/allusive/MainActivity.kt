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
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.support.annotation.IdRes
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.graphics.drawable.DrawerArrowDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.afollestad.materialdialogs.color.ColorChooserDialog
import com.afollestad.materialdialogs.folderselector.FileChooserDialog
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar
import org.apache.commons.io.FileUtils
import sheetrock.panda.changelog.ChangeLog
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        ColorChooserDialog.ColorCallback, FileChooserDialog.FileCallback {
    private var mSharedPreferences: SharedPreferences? = null
    private var mEditor: SharedPreferences.Editor? = null
    private var mExtSdDir: String? = null
    private var mTargetPath: String? = null
    private var mTag: String? = null
    private var mPointerPreviewPath: String? = null
    private var mFolderPointers: File? = null
    private var drawerArrowDrawable: DrawerArrowDrawable? = null
    private var mUtils: Utils? = null
    private val PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_Dark)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        //Firebase Analytics
        val analytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle()
        bundle.putString("Device_Name", Build.DEVICE)
        bundle.putString("AndroidVersion", Build.VERSION.CODENAME)
        analytics.logEvent("DeviceInfo", bundle)

        //Firebase Ads
        MobileAds.initialize(this, getString(R.string.banner_ad_unit_id))

        if (nav_view != null) {
            nav_view.setNavigationItemSelectedListener(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                nav_view.itemIconTintList = resources.getColorStateList(R.color.nav_state_list, theme)
            } else {
                nav_view.itemIconTintList = resources.getColorStateList(R.color.nav_state_list)
            }
        }
        checkPermissions()

        initialize()
    }

    private fun checkPermissions() {
        val permissionChecker = PermissionChecker(this)
        if (permissionChecker.lacksPermissions(*PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE -> {
                val PERMISSION_GRANTED = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (!PERMISSION_GRANTED) {
                    mUtils!!.showSnackBar(main_layout, "Please Grant Permissions", Snackbar.LENGTH_INDEFINITE, "GRANT") { view -> checkPermissions() }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    public override fun onResume() {
        super.onResume()
        checkPermissions()
        getPointer()
        setSeekBars()
    }

    private var mInterstitialAd: InterstitialAd? = null

    @SuppressLint("CommitPrefEdits")
    private fun initialize() {
        /*Load SharedPreferences**/
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        mEditor = mSharedPreferences!!.edit()

        mUtils = Utils()

        drawerArrowDrawable = DrawerArrowDrawable(this)

        setStrings()
        loadMethods()

        val mAdView = banner_ad_main
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd!!.adUnitId = getString(R.string.interstitial_ad_1_id)
        mInterstitialAd!!.loadAd(AdRequest.Builder().build())
        mInterstitialAd!!.adListener = object : AdListener() {
            override fun onAdClosed() {
                mInterstitialAd!!.loadAd(AdRequest.Builder().build())
            }
        }
    }
    private fun setToggle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            drawerArrowDrawable!!.color = resources.getColor(android.R.color.white, theme)
        } else {
            drawerArrowDrawable!!.color = resources.getColor(android.R.color.white)
        }
        toolbar.navigationIcon = drawerArrowDrawable
        toolbar.setNavigationOnClickListener { drawer_layout.openDrawer(GravityCompat.START) }
    }

    /**
     * Load or Set Strings
     */
    private fun setStrings() {
        mTag = getString(R.string.app_name)
        val pointersFolder = getString(R.string.pointerFolderName)
        mExtSdDir = Environment.getExternalStorageDirectory().toString()
        mTargetPath = mExtSdDir!! + pointersFolder
        mPointerPreviewPath = filesDir.path + "/pointerPreview.png"
    }

    /**
     * Load Methods
     */
    private fun loadMethods() {
        showChangelog()
        createPointersFolder()
        getPointer()
        setSeekBars()
        setToggle()
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

        startActivity(Intent(this@MainActivity, PointerPreview::class.java))
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
                showInstallPointersDialog()
            }
            if (mFolderPointers!!.listFiles().isEmpty()) {
                showInstallPointersDialog()
            }

            val dotNoMedia = File(mExtSdDir + File.separator + getString(R.string.app_name) + File.separator + ".nomedia")
            if (!dotNoMedia.exists()) {
                dotNoMedia.createNewFile()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

    }

    private fun showInstallPointersDialog() {
        MaterialDialog.Builder(this)
                .title("Install Pointers")
                .content("No Pointers installed. Please install some pointers")
                .positiveText("Install Pointers")
                .onPositive { dialog, which -> startActivity(Intent(this@MainActivity, ManagePointerActivity::class.java)) }.show()
    }

    /**
     * get current current pointer
     */
    private fun getPointer() {
        try {
            val pointerPath = mSharedPreferences!!.getString(getString(R.string.key_pointerPath), null)
            val selectedPointerPath = mSharedPreferences!!.getString(getString(R.string.key_selectedPointerPath), null)
            if (pointerPath != null) {
                current_pointer.setImageDrawable(Drawable.createFromPath(pointerPath))
                hideView(R.id.text_no_pointer_applied)
            } else {
                showView(R.id.text_no_pointer_applied)
                hideView(current_pointer)
            }

            if (selectedPointerPath != null) {
                selected_pointer.setImageDrawable(Drawable.createFromPath(selectedPointerPath))
            }
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }

    }

    private fun hideView(@IdRes id: Int) {
        try {
            findViewById<View>(id).visibility = View.GONE
        } catch (npe: NullPointerException) {
            npe.printStackTrace()
        }

    }

    private fun showView(@IdRes id: Int) {
        try {
            findViewById<View>(id).visibility = View.VISIBLE
        } catch (npe: NullPointerException) {
            npe.printStackTrace()
        }

    }

    private fun hideView(view: View?) {
        try {
            view!!.visibility = View.GONE
        } catch (npe: NullPointerException) {
            npe.printStackTrace()
        }

    }

    private fun showView(view: View?) {
        try {
            view!!.visibility = View.VISIBLE
        } catch (npe: NullPointerException) {
            npe.printStackTrace()
        }

    }

    fun showPointerChooser(view: View) {
        val files = File(mTargetPath!!).listFiles()
        if (files.isNotEmpty()) {
            val pointerAdapter = Utils.PointerAdapter(this)
            ObjectAnimator.ofFloat(drawerArrowDrawable!!, "progress", 0f,1f).start()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                drawerArrowDrawable!!.color = resources.getColor(android.R.color.white, theme)
            } else {
                drawerArrowDrawable!!.color = resources.getColor(android.R.color.white)
            }
            toolbar.navigationIcon = drawerArrowDrawable
            toolbar.setNavigationOnClickListener { bottom_sheet_main.dismissSheet() }

            toolbar.setTitle(R.string.text_choose_pointer)

            bottom_sheet_main.showWithSheetView(LayoutInflater
                    .from(applicationContext)
                    .inflate(R.layout.gridview_bottomsheet, bottom_sheet_main, false))
            bottom_sheet_main.addOnSheetDismissedListener {
                toolbar.title = getString(R.string.app_name)
                ObjectAnimator.ofFloat(drawerArrowDrawable!!, "progress", 1f,0f).start()
                pointerAdapter.clear()
                toolbar.setNavigationOnClickListener { view12 -> drawer_layout.openDrawer(GravityCompat.START) }
            }
            val gridView = findViewById<GridView>(R.id.bs_gridView)

            mUtils!!.loadToBottomSheetGrid(this, gridView, mTargetPath) { adapterView, view13, i, l ->
                mEditor!!.putString(getString(R.string.key_selectedPointerPath), pointerAdapter.getPath(i)).apply()
                selected_pointer.setImageDrawable(Drawable.createFromPath(pointerAdapter.getPath(i)))
                bottom_sheet_main.dismissSheet()
                Log.d(mTag, "Selected Pointer Path: " + pointerAdapter.getPath(i))
            }
            gridView?.setOnItemLongClickListener { adapterView, view14, i, l ->
                bottom_sheet_main.dismissSheet()
                val file = File(pointerAdapter.getPath(i))
                MaterialDialog.Builder(this@MainActivity)
                        .title(getString(R.string.text_delete) + file.name)
                        .content(R.string.text_delete_confirm)
                        .positiveText(R.string.text_yes)
                        .onPositive { dialog, which ->
                            if (file.delete()) {
                                mUtils!!.showSnackBar(main_layout, "Pointer deleted.")
                            } else {
                                mUtils!!.showSnackBar(main_layout, "Error deleting pointer.")
                            }
                        }
                        .negativeText(R.string.text_no)
                        .show()

                false
            }
        } else {
            showInstallPointersDialog()
        }
    }

    private fun setPointerImageParams(size: Int, padding: Int, isApplyPadding: Boolean) {
        selected_pointer.layoutParams = LinearLayout.LayoutParams(size, size)
        if (isApplyPadding) {
            selected_pointer.setPadding(padding, padding, padding, padding)
        }
    }

    private val minSize: Int
        get() = if (mUtils!!.getDpi(this) <= 240) {
            49
        } else {
            66
        }

    /**
     * Set initial values to SeekBar
     */
    private fun setSeekBars() {
        val maxSize = mSharedPreferences!!.getString(getString(R.string.key_maxPointerSize), "100")
        val maxPadding = mSharedPreferences!!.getString(getString(R.string.key_maxPaddingSize), "25")
        val alpha = mSharedPreferences!!.getInt("pointerAlpha", 255)
        val pointerSize = mSharedPreferences!!.getInt(getString(R.string.key_pointerSize), pointerSizeBar.min)
        val padding = mSharedPreferences!!.getInt(getString(R.string.key_pointerPadding), 0)
        val formatTextSize = "%s: %d*%d "
        val formatPadding = "| %s: %d "

        pointerSizeBar.min = minSize

        val alphaBarContainer = findViewById<RelativeLayout>(R.id.alpha_bar_container)
        if (mSharedPreferences!!.getBoolean(getString(R.string.key_EnablePointerAlpha), false)) {
            if (alphaBarContainer != null) {
                showView(alphaBarContainer)
                showView(text_alpha)
            }
        } else {
            if (alphaBarContainer != null) {
                hideView(alphaBarContainer)
                hideView(text_alpha)
            }
        }
        selected_pointer.setAlpha(alpha)

        //pointer size
        pointerSizeBar.max = Integer.valueOf(maxSize)!!
        text_size.text = String.format(Locale.US, formatTextSize, getString(R.string.text_size), pointerSize, pointerSize)

        //pointer padding
        seekBarPadding.max = Integer.valueOf(maxPadding)!!
        seekBarPadding.progress = padding
        text_padding.text = String.format(Locale.US, formatPadding, getString(R.string.text_padding), padding)

        //pointer alpha
        text_alpha!!.text = String.format(Locale.US, formatPadding, getString(R.string.text_alpha), alpha)
        seekBarAlpha.progress = alpha

        setPointerImageParams(pointerSize, padding, true)
        setPointerSizeBarProgress(pointerSize)

        pointerSizeBar.setOnProgressChangeListener(object : DiscreteSeekBar.OnProgressChangeListener {
            internal var imageSize: Int = 0
            override fun onProgressChanged(discreteSeekBar: DiscreteSeekBar, size: Int, b: Boolean) {
                mEditor!!.putInt(getString(R.string.key_pointerSize), size).apply()
                text_size.text = String.format(Locale.US, formatTextSize, getString(R.string.text_size), size, size)
                imageSize = size
                setPointerImageParams(size, seekBarPadding.progress, false)
            }

            override fun onStartTrackingTouch(discreteSeekBar: DiscreteSeekBar) {}

            override fun onStopTrackingTouch(discreteSeekBar: DiscreteSeekBar) {
                setPointerImageParams(imageSize, seekBarPadding.progress, false)
            }
        })

        seekBarPadding.setOnProgressChangeListener(object : DiscreteSeekBar.OnProgressChangeListener {
            internal var imagePadding: Int = 0
            override fun onProgressChanged(seekBar: DiscreteSeekBar, value: Int, fromUser: Boolean) {
                mEditor!!.putInt(getString(R.string.key_pointerPadding), value).apply()
                text_padding!!.text = String.format(Locale.US, formatPadding, getString(R.string.text_padding), value)
                setPointerImageParams(pointerSizeBar.progress, value, true)
                imagePadding = value
            }

            override fun onStartTrackingTouch(seekBar: DiscreteSeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: DiscreteSeekBar) {
                setPointerImageParams(pointerSizeBar.progress, imagePadding, true)
            }
        })

        seekBarAlpha.setOnProgressChangeListener(object : DiscreteSeekBar.OnProgressChangeListener {
            override fun onProgressChanged(seekBar: DiscreteSeekBar, value: Int, fromUser: Boolean) {
                mEditor!!.putInt("pointerAlpha", value).apply()
                text_alpha!!.text = String.format(Locale.US, formatPadding, getString(R.string.text_alpha), value)
                selected_pointer.setAlpha(value)
            }

            override fun onStartTrackingTouch(seekBar: DiscreteSeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: DiscreteSeekBar) {

            }
        })
    }

    /**
     * @param progress Integer value to be set as progress.
     */
    private fun setPointerSizeBarProgress(progress: Int) {
        pointerSizeBar.progress = progress
    }

    /**Show color picker dialog. */
    private fun showColorPicker() {
        val keyOldColor = getString(R.string.key_oldColor)
        val old_color = mSharedPreferences!!.getInt(keyOldColor, -1)
        val dialog = AmbilWarnaDialog(this, old_color, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog) {
                mUtils!!.showSnackBar(main_layout, getString(R.string.text_color_not_changed))
            }

            override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                selected_pointer.setColorFilter(color)
                mUtils!!.showSnackBar(main_layout, getString(R.string.text_color_changed))
                mEditor!!.putInt(keyOldColor, color)
                mEditor!!.apply()
            }
        })
        dialog.show()
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
            val d = Drawable.createFromPath(pointerPath)
            showView(current_pointer)
            hideView(R.id.text_no_pointer_applied)
            current_pointer.setImageDrawable(d)
            showRebootDialog()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

    }

    /**Show a reboot confirm dialog */
    private fun showRebootDialog() {
        MaterialDialog.Builder(this)
                .title(R.string.reboot)
                .theme(Theme.DARK)
                .content(R.string.text_reboot_confirm)
                .positiveText(R.string.reboot)
                .negativeText(R.string.text_no)
                .neutralText(R.string.text_soft_reboot)
                .onPositive { dialog, which ->
                    try {
                        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot"))
                        process.waitFor()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .onNeutral { dialog, which ->
                    try {
                        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "busybox killall system_server"))
                        process.waitFor()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .show()
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

    fun changeSeekVal(seek: View) {
        val progress = pointerSizeBar.progress
        val padding = seekBarPadding.progress

        when (seek.id) {
            R.id.butPlus -> setPointerSizeBarProgress(progress + 1)
            R.id.butMinus -> setPointerSizeBarProgress(progress - 1)
            R.id.butPaddingPlus -> seekBarPadding.progress = padding + 1
            R.id.butPaddingMinus -> seekBarPadding.progress = padding - 1
            R.id.butAlphaMinus -> seekBarAlpha.progress = seekBarAlpha.progress - 1
            R.id.butAlphaPlus -> seekBarAlpha.progress = seekBarAlpha.progress + 1
        }
    }

    override fun onColorSelection(dialog: ColorChooserDialog, selectedColor: Int) {
        selected_pointer.setColorFilter(selectedColor)
        mUtils!!.showSnackBar(main_layout, getString(R.string.text_color_changed))
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
                .show()
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
                .onNeutral { dialog, which -> showPreview() }
                .maxIconSize(50)
                .icon(drawable)
                .onPositive { dialog, which ->
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

                    mUtils!!.showSnackBar(main_layout, getString(R.string.text_pointer_applied))
                }.show()
    }

    override fun onFileSelection(dialog: FileChooserDialog, file: File) {
        mUtils!!.showSnackBar(main_layout, getString(R.string.text_selected_pointer) + ": " + file.name)
        selected_pointer.setImageDrawable(Drawable.createFromPath(file.absolutePath))

        if (File(mTargetPath!! + file.name).exists()) {
            mUtils!!.showSnackBar(main_layout, getString(R.string.text_pointer_exists))
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
        val drawer_layout = findViewById<DrawerLayout>(R.id.drawer_layout)
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
                    setPointerSizeBarProgress(minSize)
                    seekBarPadding.progress = 0
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
                mUtils!!.showSnackBar(main_layout, getString(R.string.text_color_changed_default))
            }
            R.id.settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.preview -> showPreview()
            R.id.about -> startActivity(Intent(this, AboutActivity::class.java))
            R.id.import_pointer -> FileChooserDialog.Builder(this)
                    .mimeType("image/*")
                    .show()
            R.id.manage_pointers -> startActivity(Intent(this, ManagePointerActivity::class.java))
        }

        if (drawer_layout != null) {
            drawer_layout.closeDrawer(GravityCompat.START)
        }
        return true
    }

    companion object {

        private val REQUEST_CODE = 0
    }
}
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

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.ColorInt
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.afollestad.materialdialogs.color.ColorChooserDialog
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import kotlinx.android.synthetic.main.pointer_preview.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class PointerPreview : AppCompatActivity(), ColorChooserDialog.ColorCallback {
    private var previewLayout: RelativeLayout? = null
    private var previewPointer: ImageView? = null
    private var previewMain: LinearLayout? = null
    private var mSharedPreferences: SharedPreferences? = null
    private var mEditor: SharedPreferences.Editor? = null
    private var mUtils: Helper? = null
    private var mInterstitialAd: InterstitialAd? = null

    private val oldColor: Int
        get() = mSharedPreferences!!.getInt("PREVIEW_OLD_COLOR", 16777215)

    private val oldPointerColor: Int
        get() = mSharedPreferences!!.getInt("PREVIEW_POINTER_OLD_COLOR", -1)

    @SuppressLint("CommitPrefEdits", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pointer_preview)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        preview_fab_apply.setOnClickListener { showSureDialog() }

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        mEditor = mSharedPreferences!!.edit()

        mUtils = Helper

        previewLayout = findViewById(R.id.preview_layout)
        previewPointer = findViewById(R.id.preview_pointer)
        previewMain = findViewById(R.id.previewMain)
        if (previewMain != null) {
            previewMain!!.visibility = View.INVISIBLE
        }

        previewLayout!!.setBackgroundColor(oldColor)

        val pointerpreviewPath = filesDir.path + "/pointerPreview.png"
        val d = Drawable.createFromPath(pointerpreviewPath)
        previewPointer!!.setImageDrawable(d)
        val pointersize = mSharedPreferences!!.getInt("POINTER_SIZE", 49)

        previewPointer!!.layoutParams = LinearLayout.LayoutParams(pointersize, pointersize)


        previewLayout!!.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    previewMain!!.visibility = View.VISIBLE
                    val x = event.x.toInt()
                    val y = event.y.toInt()
                    previewMain!!.setPadding(x, y, 0, 0)
                }
                MotionEvent.ACTION_UP -> {
                    view.performClick()
                }
            }
            true
        }

        try {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        } catch (npe: NullPointerException) {
            npe.printStackTrace()
        }

        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd!!.adUnitId = getString(R.string.interstitial_ad_1_id)
        mInterstitialAd!!.loadAd(AdRequest.Builder().build())
        mInterstitialAd!!.adListener = object : AdListener() {
            override fun onAdClosed() {
                mInterstitialAd!!.loadAd(AdRequest.Builder().build())
            }
        }
    }

    fun changePreviewBack(view: View) {
        ColorChooserDialog.Builder(this, R.string.choose_back_color)
                .titleSub(R.string.choose_back_color)
                .accentMode(false)
                .allowUserColorInputAlpha(true)
                .dynamicButtonColor(false)
                .preselect(oldColor)
                .show()
    }

    override fun onColorSelection(dialog: ColorChooserDialog, @ColorInt selectedColor: Int) {
        if (dialog.title == R.string.choose_back_color) {
            previewLayout!!.setBackgroundColor(selectedColor)
            mEditor!!.putInt("PREVIEW_OLD_COLOR", selectedColor)
        } else if (dialog.title == R.string.choose_pointer_color) {
            previewPointer!!.setColorFilter(selectedColor)
            mEditor!!.putInt("PREVIEW_POINTER_OLD_COLOR", selectedColor)
        }
        mEditor!!.apply()
    }

    override fun onColorChooserDismissed(dialog: ColorChooserDialog) {

    }

    fun changePointerBack(view: View) {
        ColorChooserDialog.Builder(this, R.string.choose_pointer_color)
                .titleSub(R.string.choose_pointer_color)
                .accentMode(false)
                .allowUserColorInputAlpha(true)
                .dynamicButtonColor(false)
                .preselect(oldPointerColor)
                .show()
    }

    private fun showSureDialog() {
        val drawable = previewPointer!!.drawable
        MaterialDialog.Builder(this)
                .title("Are You Sure?")
                .theme(Theme.DARK)
                .content("Do you want to apply this pointer?")
                .positiveText("Yes")
                .negativeText("No")
                .maxIconSize(50)
                .icon(drawable)
                .onPositive { dialog, which ->
                    try {
                        confirm()
                        if (mInterstitialAd!!.isLoaded) {
                            mInterstitialAd!!.show()
                        } else {
                            Log.d(MainActivity::class.java.simpleName, "The interstitial wasn't loaded yet.")
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    mUtils!!.showSnackBar(previewLayout!!, "Pointer Applied ")
                }.show()
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

    @Throws(IOException::class)
    private fun confirm() {
        val pointerPath = filesDir.path + "/pointer.png"
        mEditor!!.putString(getString(R.string.key_pointerPath), pointerPath)
        mEditor!!.apply()
        val bitmap = loadBitmapFromView(previewPointer)
        val file = File(pointerPath)
        Runtime.getRuntime().exec("chmod 666 " + pointerPath)
        val out: FileOutputStream
        try {
            out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
            showRebootDialog()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

    }

    private fun showRebootDialog() {
        val textReboot = getString(R.string.reboot)
        MaterialDialog.Builder(this)
                .title(textReboot)
                .theme(Theme.DARK)
                .content("All Changed applied. Do you want to Reboot?")
                .positiveText(textReboot)
                .negativeText("Cancel")
                .neutralText("Soft " + textReboot)
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
                .show()
    }

    fun resetColor(view: View) {
        previewPointer!!.colorFilter = null
    }
}

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

package com.afterroot.allusive.fragment

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.graphics.drawable.DrawerArrowDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.allusive.Helper
import com.afterroot.allusive.MainActivity
import com.afterroot.allusive.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_main.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class MainFragment: Fragment() {

    private var sharedPreferences: SharedPreferences? = null
    private var editor: SharedPreferences.Editor? = null
    private var fragmentView: View? = null
    private var extSdDir: String? = null
    private var targetPath: String? = null
    private var pointerPreviewPath: String? = null
    private var TAG: String = this::class.java.simpleName
    private var drawerArrowDrawable: DrawerArrowDrawable? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = inflater?.inflate(R.layout.fragment_main, container, false)
        return fragmentView
    }

    @SuppressLint("CommitPrefEdits")
    override fun onStart() {
        super.onStart()

        sharedPreferences = Helper.getSharedPreferences(activity)
        editor = sharedPreferences!!.edit()

        init()
    }

    private fun init(){
        val pointersFolder = getString(R.string.pointer_folder_path)
        extSdDir = Environment.getExternalStorageDirectory().toString()
        targetPath = extSdDir!! + pointersFolder
        pointerPreviewPath = activity.filesDir.path + "/pointerPreview.png"
        drawerArrowDrawable = MainActivity.arrowDrawable

        activity.card_new_pointer.setOnClickListener { showPointerChooser() }

        MobileAds.initialize(activity, getString(R.string.banner_ad_unit_id))

        val adView = activity.banner_ad_main
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        getPointer()

        activity.fab_apply.setOnClickListener {
            applyPointer()
        }
    }

    /**
     * @throws IOException exception
     */
    @Throws(IOException::class)
    private fun applyPointer() {
        val pointerPath = activity.filesDir.path + "/pointer.png"
        editor!!.putString(getString(R.string.key_pointerPath), pointerPath).apply()
        val bitmap = loadBitmapFromView(selected_pointer)
        val file = File(pointerPath)
        Runtime.getRuntime().exec("chmod 666 $pointerPath")
        val out: FileOutputStream
        try {
            out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
            Helper.showView(current_pointer)
            Helper.hideView(text_no_pointer_applied)
            current_pointer.setImageDrawable(Drawable.createFromPath(pointerPath))
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return
        }
        Helper.showSnackBar(activity.coordinator_layout, "Pointer Applied", Snackbar.LENGTH_LONG, "REBOOT", View.OnClickListener {
            MaterialDialog.Builder(activity)
                    .title(R.string.reboot)
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
                    .show()
        })
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

    private fun showPointerChooser() {
        val pointerFragment = PointerBottomSheetFragment()
        pointerFragment.show(activity.supportFragmentManager, "POINTERS")
        pointerFragment.setTitle("Select Pointers")
        pointerFragment.setPointerSelectCallback(object: PointerBottomSheetFragment.PointerSelectCallback{
            override fun onPointerSelected(pointerPath: String) {
                editor!!.putString(getString(R.string.key_selectedPointerPath), pointerPath).apply()
                activity.selected_pointer.setImageDrawable(Drawable.createFromPath(pointerPath))
                Log.d(this::class.java.simpleName, "Selected Pointer Path: $pointerPath" )
            }

        })
    }

    private fun getPointer() {
        try {
            val pointerPath = sharedPreferences!!.getString(getString(R.string.key_pointerPath), null)
            val selectedPointerPath = sharedPreferences!!.getString(getString(R.string.key_selectedPointerPath), null)
            if (pointerPath != null) {
                current_pointer.setImageDrawable(Drawable.createFromPath(pointerPath))
                Helper.hideView(text_no_pointer_applied)
            } else {
                Helper.showView(text_no_pointer_applied)
                Helper.hideView(current_pointer)
            }

            if (selectedPointerPath != null) {
                selected_pointer.setImageDrawable(Drawable.createFromPath(selectedPointerPath))
            }
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }
    }

     companion object {
         fun newInstance(): MainFragment {
             return MainFragment()
         }

    }
}
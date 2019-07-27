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

package com.afterroot.allusive.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.allusive.GlideApp
import com.afterroot.allusive.R
import com.afterroot.allusive.ui.SplashActivity
import com.afterroot.allusive.utils.getDrawableExt
import com.afterroot.allusive.utils.getMinPointerSize
import com.afterroot.allusive.utils.getPrefs
import com.afterroot.allusive.utils.visible
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.fragment_main.*
import org.jetbrains.anko.design.longSnackbar
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class MainFragment : Fragment() {

    private val _tag = "MainFragment"
    private var extSdDir: String? = null
    private var fragmentView: View? = null
    private var pointerPreviewPath: String? = null
    private var sharedPreferences: SharedPreferences? = null
    private var targetPath: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(_tag, "adding view..")
        setHasOptionsMenu(true)
        fragmentView = inflater.inflate(R.layout.fragment_main, container, false)
        return fragmentView
    }

    @SuppressLint("CommitPrefEdits")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = context!!.getPrefs()

        init()
    }

    private fun init() {
        val pointersFolder = getString(R.string.pointer_folder_path)
        extSdDir = Environment.getExternalStorageDirectory().toString()
        targetPath = extSdDir!! + pointersFolder
        pointerPreviewPath = activity!!.filesDir.path + "/pointerPreview.png"

        activity!!.card_new_pointer.setOnClickListener { showPointerChooser() }

        MobileAds.initialize(activity!!, getString(R.string.ad_banner_unit_id))

        val adView = activity!!.banner_ad_main
        val adRequest = AdRequest.Builder().addTestDevice("C2E7A1508F5C10E8CAD48853E334BD4C").build()
        adView.loadAd(adRequest)

        getPointer()

        activity!!.fab_apply.apply {
            setOnClickListener {
                applyPointer()
            }
            icon = context!!.getDrawableExt(R.drawable.ic_action_apply)
        }
    }

    /**
     * @throws IOException exception
     */
    @Throws(IOException::class)
    private fun applyPointer() {
        val pointerPath = activity!!.filesDir.path + "/pointer.png"
        sharedPreferences!!.edit(true) {
            putString(getString(R.string.key_pointerPath), pointerPath)
        }
        val bitmap = loadBitmapFromView(selected_pointer)
        val file = File(pointerPath)
        Runtime.getRuntime().exec("chmod 666 $pointerPath")
        val out: FileOutputStream
        try {
            out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
            current_pointer.visible(true)
            text_no_pointer_applied.visible(false)
            current_pointer.setImageDrawable(Drawable.createFromPath(pointerPath))
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return
        }
        activity!!.container.longSnackbar(
            message = getString(R.string.text_pointer_applied),
            actionText = getString(R.string.reboot)
        ) {
            MaterialDialog(activity!!).show {
                title(res = R.string.reboot)
                message(res = R.string.text_reboot_confirm)
                positiveButton(res = R.string.reboot) {
                    try {
                        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot"))
                        process.waitFor()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                negativeButton(res = R.string.text_soft_reboot) {
                    try {
                        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "busybox killall system_server"))
                        process.waitFor()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }.anchorView = activity!!.navigation
    }

    private fun loadBitmapFromView(view: View): Bitmap {
        val b = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        view.run {
            layout(view.left, view.top, view.right, view.bottom)
            draw(c)
        }
        return b
    }

    private fun showPointerChooser() {
        val pointerFragment = PointerBottomSheetFragment()
        pointerFragment.show(activity!!.supportFragmentManager, "POINTERS")
        pointerFragment.setTitle("Select Pointers")
        pointerFragment.setPointerSelectCallback(object : PointerBottomSheetFragment.PointerSelectCallback {
            override fun onPointerSelected(pointerPath: String) {
                sharedPreferences!!.edit(true) {
                    putString(getString(R.string.key_selectedPointerPath), pointerPath)
                }
                GlideApp.with(context!!).load(File(pointerPath)).into(activity!!.selected_pointer)
                Log.d(_tag, "onPointerSelected: Selected Pointer Path: $pointerPath")
            }

        })
    }

    private fun getPointer() {
        try {
            val pointerPath = sharedPreferences!!.getString(getString(R.string.key_pointerPath), null)
            val selectedPointerPath = sharedPreferences!!.getString(getString(R.string.key_selectedPointerPath), null)
            if (pointerPath != null) {
                GlideApp.with(context!!)
                    .load(File(pointerPath))
                    .override(context!!.getMinPointerSize())
                    .into(current_pointer)
                text_no_pointer_applied.visible(false)
            } else {
                text_no_pointer_applied.visible(true)
                current_pointer.visible(false)
            }

            if (selectedPointerPath != null) {
                GlideApp.with(context!!)
                    .load(File(selectedPointerPath))
                    .override(context!!.getMinPointerSize())
                    .into(selected_pointer)
            }
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_dashboard_activity, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.profile_logout -> {
                signOutDialog().show()
            }
            else -> {
                return item.onNavDestinationSelected(
                    activity!!.findNavController(R.id.fragment_repo_nav)
                ) || super.onOptionsItemSelected(
                    item
                )
            }
        }
        return true
    }

    private fun signOutDialog(): AlertDialog.Builder {
        return AlertDialog.Builder(context!!)
            .setTitle(getString(R.string.dialog_title_sign_out))
            .setMessage(getString(R.string.dialog_msg_sign_out))
            .setPositiveButton(R.string.dialog_title_sign_out) { _, _ ->
                AuthUI.getInstance().signOut(context!!).addOnSuccessListener {
                    Toast.makeText(
                        context!!,
                        getString(R.string.dialog_sign_out_result_success),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    startActivity(Intent(context!!, SplashActivity::class.java))
                }
            }
            .setNegativeButton(R.string.dialog_button_cancel) { _, _ ->

            }.setCancelable(true)
    }
}
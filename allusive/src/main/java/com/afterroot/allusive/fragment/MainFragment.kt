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

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.ui.onNavDestinationSelected
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afterroot.allusive.*
import com.afterroot.allusive.Constants.POINTER_MOUSE
import com.afterroot.allusive.Constants.POINTER_TOUCH
import com.afterroot.allusive.adapter.PointerAdapter
import com.afterroot.allusive.adapter.PointerAdapterDelegate
import com.afterroot.allusive.adapter.callback.ItemSelectedCallback
import com.afterroot.allusive.database.DatabaseFields
import com.afterroot.allusive.database.MyDatabase
import com.afterroot.allusive.model.Pointer
import com.afterroot.allusive.model.RoomPointer
import com.afterroot.allusive.ui.SplashActivity
import com.afterroot.core.extensions.getDrawableExt
import com.afterroot.core.extensions.loadBitmapFromView
import com.afterroot.core.extensions.visible
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.layout_grid_bottomsheet.view.*
import kotlinx.android.synthetic.main.layout_grid_bottomsheet.view.bs_button_install_pointers
import kotlinx.android.synthetic.main.layout_grid_bottomsheet.view.info_no_pointer_installed
import kotlinx.android.synthetic.main.layout_list_bottomsheet.view.*
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.snackbar
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class MainFragment : Fragment() {

    private val _tag = "MainFragment"
    private var extSdDir: String? = null
    private var fragmentView: View? = null
    private var pointerPreviewPath: String? = null
    private var targetPath: String? = null
    private lateinit var interstitialAd: InterstitialAd
    private val myDatabase: MyDatabase by inject()
    private lateinit var settings: Settings

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        fragmentView = inflater.inflate(R.layout.fragment_main, container, false)
        return fragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settings = Settings(this.context!!)

        init()
    }

    private fun init() {
        val pointersFolder = getString(R.string.pointer_folder_path)
        extSdDir = Environment.getExternalStorageDirectory().toString()
        targetPath = extSdDir!! + pointersFolder
        pointerPreviewPath = activity!!.filesDir.path + "/pointerPreview.png"

        activity!!.apply {
            layout_new_pointer.setOnClickListener { showListPointerChooser(pointerType = POINTER_TOUCH) }
            layout_new_mouse.setOnClickListener {
                showListPointerChooser(
                    pointerType = POINTER_MOUSE,
                    title = getString(R.string.dialog_title_select_mouse_pointer)
                )
            }
            fab_apply.apply {
                setOnClickListener {
                    applyPointer()
                }
                icon = context!!.getDrawableExt(R.drawable.ic_action_apply)
            }
            setUpAd()

            loadCurrentPointers()

            action_customize.setOnClickListener {
                if (!isPointerSelected()) {
                    activity!!.container.snackbar(
                        message = getString(R.string.msg_pointer_not_selected)
                    ).anchorView = activity!!.navigation
                } else {
                    val bundle = Bundle().apply {
                        putInt("TYPE", POINTER_TOUCH)
                    }
                    val extras = FragmentNavigatorExtras(selected_pointer to getString(R.string.main_fragment_transition))
                    findNavController(R.id.fragment_repo_nav).navigate(R.id.customizeFragment, bundle, null, extras)
                }
            }

            action_customize_mouse.setOnClickListener {
                if (!isMouseSelected()) {
                    activity!!.container.snackbar(
                        message = getString(R.string.msg_mouse_not_selected)
                    ).anchorView = activity!!.navigation
                } else {
                    val bundle = Bundle().apply {
                        putInt("TYPE", POINTER_MOUSE)
                    }
                    val extras = FragmentNavigatorExtras(selected_mouse to getString(R.string.transition_mouse))
                    findNavController(R.id.fragment_repo_nav).navigate(R.id.customizeFragment, bundle, null, extras)
                }
            }
        }
    }

    private fun isPointerSelected(): Boolean {
        if (selected_pointer.width == 0 || selected_pointer.height == 0) {
            return false
        }
        return true
    }

    private fun isMouseSelected(): Boolean {
        if (selected_mouse.width == 0 || selected_mouse.height == 0) {
            return false
        }
        return true
    }

    private fun loadCurrentPointers() {
        val pointerPath = settings.pointerPath
        val mousePath = settings.mousePath
        val selectedPointerPath = settings.selectedPointerPath
        val selectedMousePath = settings.selectedMousePath
        var size = settings.pointerSize
        var mouseSize = settings.mouseSize
        val padding = settings.pointerPadding
        val mousePadding = settings.mousePadding
        val pointerColor = settings.pointerColor
        val mouseColor = settings.mouseColor

        try {
            if (size <= 0) {
                size = context!!.getMinPointerSize()
            }
            if (mouseSize <= 0) {
                mouseSize = context!!.getMinPointerSize()
            }
            if (pointerPath != null) {
                current_pointer.setImageDrawable(Drawable.createFromPath(pointerPath))
                text_no_pointer_applied.visible(false)
            } else {
                text_no_pointer_applied.visible(true)
                current_pointer.visible(false)
            }

            if (mousePath != null) {
                current_mouse.setImageDrawable(Drawable.createFromPath(mousePath))
                text_no_mouse_applied.visible(false)
            } else {
                text_no_mouse_applied.visible(true)
                current_mouse.visible(false)
            }

            if (selectedPointerPath != null) {
                selected_pointer.apply {
                    layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
                    setPadding(padding, padding, padding, padding)
                    setColorFilter(pointerColor)
                    imageAlpha = if (settings.isEnableAlpha) settings.pointerAlpha else 255
                }
                GlideApp.with(context!!)
                    .load(File(selectedPointerPath))
                    .into(selected_pointer)
            }
            if (selectedMousePath != null) {
                selected_mouse.apply {
                    layoutParams = FrameLayout.LayoutParams(mouseSize, mouseSize, Gravity.CENTER)
                    setPadding(mousePadding, mousePadding, mousePadding, mousePadding)
                    setColorFilter(mouseColor)
                    imageAlpha = if (settings.isEnableAlpha) settings.mouseAlpha else 255
                }
                GlideApp.with(context!!)
                    .load(File(selectedMousePath))
                    .into(selected_mouse)
            }
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun applyPointer() {
        if (!isPointerSelected()) {
            activity!!.container.snackbar(
                message = getString(R.string.msg_pointer_not_selected)
            ).anchorView = activity!!.navigation
            return
        }
        if (!isMouseSelected()) {
            activity!!.container.snackbar(
                message = getString(R.string.msg_mouse_not_selected)
            ).anchorView = activity!!.navigation
            return
        }
        val filesDir = activity!!.filesDir.path
        val pointerPath = "$filesDir/pointer.png"
        val mousePath = "$filesDir/mouse.png"
        settings.pointerPath = pointerPath
        settings.mousePath = mousePath
        createFileFromView(selected_pointer, pointerPath)
        createFileFromView(selected_mouse, mousePath)

        text_no_pointer_applied.visible(false)
        text_no_mouse_applied.visible(false)
        current_pointer.apply {
            visible(true)
            setImageDrawable(Drawable.createFromPath(pointerPath))
        }
        current_mouse.apply {
            visible(true)
            setImageDrawable(Drawable.createFromPath(mousePath))
        }

        interstitialAd.apply {
            if (isLoaded) show() else {
                activity!!.container.longSnackbar(
                    message = getString(R.string.text_pointer_applied),
                    actionText = getString(R.string.reboot)
                ) {
                    showRebootDialog()
                }.anchorView = activity!!.navigation
            }
            adListener = object : AdListener() {
                override fun onAdClosed() {
                    super.onAdClosed()
                    interstitialAd.loadAd(AdRequest.Builder().build())
                    activity!!.container.longSnackbar(
                        message = getString(R.string.text_pointer_applied),
                        actionText = getString(R.string.reboot)
                    ) {
                        showRebootDialog()
                    }.anchorView = activity!!.navigation
                }
            }
        }
    }

    private fun showRebootDialog() {
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
    }

    private fun setUpAd() {
        val adRequest = AdRequest.Builder()
        if (BuildConfig.DEBUG) {
            adRequest.addTestDevice(BuildConfig.AD_TEST_DEVICE_ID)
        }
        banner_ad_main.loadAd(adRequest.build())

        interstitialAd = InterstitialAd(this.activity!!)
        interstitialAd.apply {
            adUnitId = if (BuildConfig.DEBUG) {
                getString(R.string.ad_interstitial_test_id)
            } else {
                getString(R.string.ad_interstitial_1_id)
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    private fun createFileFromView(view: View, exportPath: String) {
        val file = File(exportPath)
        Runtime.getRuntime().exec("chmod 666 $exportPath")
        val out: FileOutputStream
        try {
            out = FileOutputStream(file)
            loadBitmapFromView(view)?.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return
        }
    }

    private suspend fun generateRoomPointerFromFileName(fileNames: List<String>) {
        val roomPointers = arrayListOf<RoomPointer>()
        fileNames.forEach { filename ->
            get<FirebaseFirestore>().collection(DatabaseFields.COLLECTION_POINTERS)
                .whereEqualTo(DatabaseFields.FIELD_FILENAME, filename).get().addOnSuccessListener { snapshot ->
                    if (!snapshot.isEmpty) { //Pointer available in repository
                        val p = snapshot.documents[0].toObject(Pointer::class.java)!!
                        var id = ""
                        var name = ""
                        p.uploadedBy!!.forEach {
                            id = it.key
                            name = it.value
                        }
                        val pointer = RoomPointer(
                            file_name = p.filename,
                            pointer_desc = p.description,
                            pointer_name = p.name,
                            uploader_id = id,
                            uploader_name = name
                        )

                        roomPointers.add(pointer)
                        lifecycleScope.launch {
                            if (myDatabase.pointerDao().exists(filename).isEmpty()) {
                                myDatabase.pointerDao().add(pointer)
                            }
                        }
                    } else { //Pointer not available in repository
                        val pointer = RoomPointer(
                            file_name = filename,
                            uploader_name = "You (Local)",
                            uploader_id = "N/A",
                            pointer_name = filename,
                            pointer_desc = "N/A"
                        )
                        lifecycleScope.launch {
                            if (myDatabase.pointerDao().exists(filename).isEmpty()) {
                                myDatabase.pointerDao().add(pointer)
                            }
                        }
                    }
                }
        }
    }

    private fun import() {
        val fileNames = arrayListOf<String>()
        File(targetPath!!).listFiles()?.forEach {
            if (it.name != ".nomedia") {
                fileNames.add(it.name)
            }

        }
        lifecycleScope.launch {
            generateRoomPointerFromFileName(fileNames)
        }
    }

    lateinit var pointerAdapter: PointerAdapterDelegate
    private fun showListPointerChooser(title: String = getString(R.string.dialog_title_select_pointer), pointerType: Int) {
        val pointersFolder = File(targetPath!!)
        val dialog = MaterialDialog(context!!, BottomSheet(LayoutMode.MATCH_PARENT)).show {
            customView(R.layout.layout_list_bottomsheet)
            title(text = title)
            noAutoDismiss()
            positiveButton(text = "Import Your Pointers") {
                import()
            }
        }

        val dialogView = dialog.getCustomView()

        pointerAdapter = PointerAdapterDelegate(object : ItemSelectedCallback {
            override fun onClick(position: Int, view: View?) {
                val selectedItem = pointerAdapter.getItem(position) as RoomPointer
                if (pointerType == POINTER_TOUCH) {
                    settings.selectedPointerPath = targetPath + selectedItem.file_name
                } else {
                    settings.selectedMousePath = targetPath + selectedItem.file_name
                }
                GlideApp.with(context!!)
                    .load(File(targetPath + selectedItem.file_name))
                    .override(context!!.getMinPointerSize())
                    .into(if (pointerType == POINTER_TOUCH) activity!!.selected_pointer else activity!!.selected_mouse)
                dialog.dismiss()
            }

            override fun onLongClick(position: Int) {
                val selectedItem = pointerAdapter.getItem(position) as RoomPointer
                val file = File(targetPath + selectedItem.file_name)
                MaterialDialog(activity!!).show {
                    title(text = "${getString(R.string.text_delete)} ${file.name}")
                    message(res = R.string.text_delete_confirm)
                    positiveButton(res = R.string.text_yes) {
                        if (file.delete()) {
                            lifecycleScope.launch {
                                myDatabase.pointerDao().delete(selectedItem)
                            }
                            Toast.makeText(context, getString(R.string.msg_delete_success), Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            Toast.makeText(context, getString(R.string.msg_delete_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                    negativeButton(res = R.string.text_no)
                }
            }

        })
        dialogView.list_pointers.apply {
            val lm = LinearLayoutManager(context!!)
            layoutManager = lm
            addItemDecoration(DividerItemDecoration(this.context, lm.orientation))
            this.adapter = pointerAdapter
        }

        lifecycleScope.launch {
            //Observe Db on CoroutineScope
            myDatabase.pointerDao().getAll().observe(this@MainFragment, Observer {
                pointerAdapter.add(it)
                //Show install msg if no pointer installed
                dialogView.apply {
                    info_no_pointer_installed.visible(pointerAdapter.getList().isEmpty())
                    text_dialog_hint.visible(pointerAdapter.getList().isNotEmpty())
                    bs_button_install_pointers.setOnClickListener {
                        dialog.dismiss()
                        activity!!.findNavController(R.id.fragment_repo_nav)
                            .navigate(R.id.repoFragment)
                    }
                    bs_button_import_pointers.setOnClickListener {
                        import()
                    }
                }
            })
        }

        val dotNoMedia = File("${targetPath}/.nomedia")
        if (!pointersFolder.exists()) {
            pointersFolder.mkdirs()
        }
        if (!dotNoMedia.exists()) {
            dotNoMedia.createNewFile()
        }
    }

    private fun showPointerChooser(title: String = getString(R.string.dialog_title_select_pointer), pointerType: Int) {
        val dialog = MaterialDialog(context!!, BottomSheet(LayoutMode.MATCH_PARENT)).show {
            customView(R.layout.layout_grid_bottomsheet)
            title(text = title)
        }

        val dialogView = dialog.getCustomView()
        val pointerAdapter = PointerAdapter(activity!!)

        /*dialogView.banner_ad_pointer_grid.apply {
            val adRequest = AdRequest.Builder().build()
            this.loadAd(adRequest)
        }*/

        dialogView.grid_pointers.apply {
            adapter = pointerAdapter
            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                if (pointerType == POINTER_TOUCH) {
                    settings.selectedPointerPath = pointerAdapter.getItem(position)
                } else {
                    settings.selectedMousePath = pointerAdapter.getItem(position)
                }
                GlideApp.with(context!!).load(File(pointerAdapter.getItem(position))).override(context!!.getMinPointerSize())
                    .into(if (pointerType == POINTER_TOUCH) activity!!.selected_pointer else activity!!.selected_mouse)
                dialog.dismiss()
            }
        }

        try {
            val pointersFolder = File(targetPath!!)
            val dotNoMedia = File("${targetPath}/.nomedia")
            if (!pointersFolder.exists()) {
                pointersFolder.mkdirs()
            }
            if (!dotNoMedia.exists()) {
                dotNoMedia.createNewFile()
            }

            val pointerFiles = pointersFolder.listFiles()//.filter { getMimeType(it.name)!!.startsWith("image/") }
            PointerAdapter.itemList.clear()
            if (pointerFiles!!.isNotEmpty()) {
                dialogView.info_no_pointer_installed.visible(false)
                pointerFiles.mapTo(PointerAdapter.itemList) { it.absolutePath }
                pointerAdapter.notifyDataSetChanged()

                dialogView.grid_pointers.onItemLongClickListener =
                    AdapterView.OnItemLongClickListener { _, _, i, _ ->
                        val file = File(pointerAdapter.getItem(i))
                        MaterialDialog(activity!!).show {
                            title(text = getString(R.string.text_delete) + " " + file.name)
                            message(res = R.string.text_delete_confirm)
                            positiveButton(res = R.string.text_yes) {
                                if (file.delete()) {
                                    Toast.makeText(context, getString(R.string.msg_delete_success), Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    Toast.makeText(context, getString(R.string.msg_delete_failed), Toast.LENGTH_SHORT).show()
                                }
                            }
                            negativeButton(res = R.string.text_no)
                        }
                        false
                    }
            } else {
                dialogView.apply {
                    info_no_pointer_installed.visible(true)
                    bs_button_install_pointers.setOnClickListener {
                        dialog.dismiss()
                        activity!!.findNavController(R.id.fragment_repo_nav)
                            .navigate(R.id.repoFragment)
                    }
                }
            }
        } catch (npe: Exception) {
            Log.e(_tag, "showPointerChooser: $npe")
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
            .setNegativeButton(android.R.string.cancel) { _, _ ->

            }.setCancelable(true)
    }
}
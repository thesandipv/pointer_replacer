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

package com.afterroot.allusive2.ui.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
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
import com.afterroot.allusive2.*
import com.afterroot.allusive2.Constants.POINTER_MOUSE
import com.afterroot.allusive2.Constants.POINTER_TOUCH
import com.afterroot.allusive2.R
import com.afterroot.allusive2.adapter.LocalPointersAdapter
import com.afterroot.allusive2.adapter.callback.ItemSelectedCallback
import com.afterroot.allusive2.database.MyDatabase
import com.afterroot.allusive2.model.RoomPointer
import com.afterroot.allusive2.ui.SplashActivity
import com.afterroot.core.extensions.getAsBitmap
import com.afterroot.core.extensions.getDrawableExt
import com.afterroot.core.extensions.visible
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.ads.*
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.layout_list_bottomsheet.view.*
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.snackbar
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class MainFragment : Fragment() {

    private lateinit var interstitialAd: InterstitialAd
    private val myDatabase: MyDatabase by inject()
    private val settings: Settings by inject()
    private var targetPath: String? = null
    private val remoteConfig: FirebaseRemoteConfig by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        targetPath = requireContext().getPointerSaveDir()

        requireActivity().apply {
            layout_new_pointer.setOnClickListener {
                showListPointerChooser(pointerType = POINTER_TOUCH)
            }
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
                icon = requireContext().getDrawableExt(R.drawable.ic_action_apply)
            }
            setUpAd()

            loadCurrentPointers()

            action_customize.setOnClickListener {
                if (!isPointerSelected()) {
                    requireActivity().container.snackbar(
                        message = getString(R.string.msg_pointer_not_selected)
                    ).anchorView = requireActivity().navigation
                } else {
                    val bundle = Bundle().apply {
                        putInt("TYPE", POINTER_TOUCH)
                    }
                    val extras =
                        FragmentNavigatorExtras(selected_pointer to getString(R.string.main_fragment_transition))
                    findNavController(R.id.fragment_repo_nav).navigate(R.id.customizeFragment, bundle, null, extras)
                }
            }

            action_customize_mouse.setOnClickListener {
                if (!isMouseSelected()) {
                    requireActivity().container.snackbar(
                        message = getString(R.string.msg_mouse_not_selected)
                    ).anchorView = requireActivity().navigation
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
                size = requireContext().getMinPointerSize()
            }
            if (mouseSize <= 0) {
                mouseSize = requireContext().getMinPointerSize()
            }
            if (pointerPath != null) {
                current_pointer.apply {
                    setImageDrawable(Drawable.createFromPath(pointerPath))
                    minimumHeight = requireContext().getMinPointerSizePx()
                    minimumWidth = requireContext().getMinPointerSizePx()
                }
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
                GlideApp.with(requireContext())
                    .load(Uri.fromFile(File(selectedPointerPath)))
                    .into(selected_pointer)
            }
            if (selectedMousePath != null) {
                selected_mouse.apply {
                    layoutParams = FrameLayout.LayoutParams(mouseSize, mouseSize, Gravity.CENTER)
                    setPadding(mousePadding, mousePadding, mousePadding, mousePadding)
                    setColorFilter(mouseColor)
                    imageAlpha = if (settings.isEnableAlpha) settings.mouseAlpha else 255
                }
                GlideApp.with(requireContext())
                    .load(Uri.fromFile(File(selectedMousePath)))
                    .into(selected_mouse)
            }
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun applyPointer() {
        if (!isPointerSelected()) {
            requireActivity().container.snackbar(
                message = getString(R.string.msg_pointer_not_selected)
            ).anchorView = requireActivity().navigation
            return
        }
        if (!isMouseSelected()) {
            requireActivity().container.snackbar(
                message = getString(R.string.msg_mouse_not_selected)
            ).anchorView = requireActivity().navigation
            return
        }
        val filesDir = requireContext().getPointerSaveRootDir()
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
                requireActivity().container.longSnackbar(
                    message = getString(R.string.text_pointer_applied),
                    actionText = getString(R.string.reboot)
                ) {
                    showRebootDialog()
                }.anchorView = requireActivity().navigation
            }
            adListener = object : AdListener() {
                override fun onAdClosed() {
                    super.onAdClosed()
                    interstitialAd.loadAd(AdRequest.Builder().build())
                    requireActivity().container.longSnackbar(
                        message = getString(R.string.text_pointer_applied),
                        actionText = getString(R.string.reboot)
                    ) {
                        showRebootDialog()
                    }.anchorView = requireActivity().navigation
                }
            }
        }
    }

    private fun showRebootDialog() { //TODO Implement libsuperuser https://github.com/Chainfire/libsuperuser
        MaterialDialog(requireActivity()).show {
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
                try { //Also try "killall zygote"
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "busybox killall system_server"))
                    process.waitFor()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setUpAd() {
        interstitialAd = InterstitialAd(this.requireActivity())
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            kotlin.runCatching {
                val adView = AdView(requireContext())
                adView.apply {
                    adSize = AdSize.BANNER
                    adUnitId = if (BuildConfig.DEBUG || (!it.isSuccessful && BuildConfig.DEBUG)) {
                        getString(R.string.ad_banner_unit_id)
                    } else remoteConfig.getString("ad_main_unit_id")

                    ad_container.addView(this)
                    loadAd(AdRequest.Builder().build())
                }

                interstitialAd.apply {
                    adUnitId = if (BuildConfig.DEBUG || (!it.isSuccessful && BuildConfig.DEBUG)) {
                        getString(R.string.ad_interstitial_1_id)
                    } else remoteConfig.getString("ad_interstitial_1_id")
                    loadAd(AdRequest.Builder().build())
                }
            }
        }
    }

    private fun createFileFromView(view: View, exportPath: String) {
        val file = File(exportPath)
        Runtime.getRuntime().exec("chmod 666 $exportPath")
        val out: FileOutputStream
        try {
            out = FileOutputStream(file)
            view.getAsBitmap()?.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return
        }
    }

    /*private suspend fun generateRoomPointerFromFileName(fileNames: List<String>) {
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

   private fun import() { //Remove this
         val fileNames = arrayListOf<String>()
         File(targetPath!!).listFiles()?.forEach {
             if (it.name != ".nomedia") {
                 fileNames.add(it.name)
             }
         }
         lifecycleScope.launch {
             generateRoomPointerFromFileName(fileNames)
         }
     }*/

    private lateinit var pointerAdapter: LocalPointersAdapter
    private fun showListPointerChooser(title: String = getString(R.string.dialog_title_select_pointer), pointerType: Int) {
        val dialog = MaterialDialog(requireContext(), BottomSheet(LayoutMode.MATCH_PARENT)).show {
            customView(R.layout.layout_list_bottomsheet)
            title(text = title)
            noAutoDismiss()
            /*positiveButton(text = "Import Your Pointers") { //Remove this
                try {
                    import()
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
            }*/
        }

        val dialogView = dialog.getCustomView()

        pointerAdapter = LocalPointersAdapter(object : ItemSelectedCallback<RoomPointer> {
            override fun onClick(position: Int, view: View?, item: RoomPointer) {
                if (pointerType == POINTER_TOUCH) {
                    settings.selectedPointerName = item.file_name
                    settings.selectedPointerPath = targetPath + item.file_name
                } else {
                    settings.selectedMouseName = item.file_name
                    settings.selectedMousePath = targetPath + item.file_name
                }
                GlideApp.with(requireContext())
                    .load(File(targetPath + item.file_name))
                    .override(requireContext().getMinPointerSize())
                    .into(if (pointerType == POINTER_TOUCH) requireActivity().selected_pointer else requireActivity().selected_mouse)

                dialog.dismiss()
            }

            override fun onLongClick(position: Int, item: RoomPointer) {
                MaterialDialog(requireActivity()).show {
                    title(text = "${getString(R.string.text_delete)} ${item.pointer_name}")
                    message(res = R.string.text_delete_confirm)
                    positiveButton(res = R.string.text_yes) {
                        if (File(targetPath + item.file_name).delete()) {
                            lifecycleScope.launch {
                                myDatabase.pointerDao().delete(item)
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
            val lm = LinearLayoutManager(requireContext())
            layoutManager = lm
            addItemDecoration(DividerItemDecoration(this.context, lm.orientation))
            this.adapter = pointerAdapter
            scheduleLayoutAnimation()
        }

        lifecycleScope.launch {
            //Observe Db on CoroutineScope
            myDatabase.pointerDao().getAll().observe(viewLifecycleOwner, {
                pointerAdapter.submitList(it)
                //Show install msg if no pointer installed
                dialogView.apply {
                    info_no_pointer_installed.visible(it.isEmpty())
                    text_dialog_hint.visible(it.isNotEmpty())
                    bs_button_install_pointers.setOnClickListener {
                        dialog.dismiss()
                        requireActivity().findNavController(R.id.fragment_repo_nav)
                            .navigate(R.id.repoFragment)
                    }
                    /*bs_button_import_pointers.setOnClickListener {
                         import()
                     }*/
                }
            })
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
                    requireActivity().findNavController(R.id.fragment_repo_nav)
                ) || super.onOptionsItemSelected(
                    item
                )
            }
        }
        return true
    }

    private fun signOutDialog(): AlertDialog.Builder {
        return AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_title_sign_out))
            .setMessage(getString(R.string.dialog_msg_sign_out))
            .setPositiveButton(R.string.dialog_title_sign_out) { _, _ ->
                AuthUI.getInstance().signOut(requireContext()).addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.dialog_sign_out_result_success),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    startActivity(Intent(requireContext(), SplashActivity::class.java))
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->

            }.setCancelable(true)
    }
}
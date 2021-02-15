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

package com.afterroot.allusive2.ui.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import com.afterroot.allusive2.*
import com.afterroot.allusive2.Constants.POINTER_MOUSE
import com.afterroot.allusive2.Constants.POINTER_TOUCH
import com.afterroot.allusive2.R
import com.afterroot.allusive2.adapter.LocalPointersAdapter
import com.afterroot.allusive2.adapter.callback.ItemSelectedCallback
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.database.MyDatabase
import com.afterroot.allusive2.databinding.FragmentMainBinding
import com.afterroot.allusive2.databinding.LayoutListBottomsheetBinding
import com.afterroot.allusive2.model.RoomPointer
import com.afterroot.allusive2.ui.SplashActivity
import com.afterroot.allusive2.viewmodel.MainSharedViewModel
import com.afterroot.core.extensions.getAsBitmap
import com.afterroot.core.extensions.getDrawableExt
import com.afterroot.core.extensions.showStaticProgressDialog
import com.afterroot.core.extensions.visible
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.ads.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.find
import org.jetbrains.anko.toast
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding
    private lateinit var interstitialAd: InterstitialAd
    private val myDatabase: MyDatabase by inject()
    private val remoteConfig: FirebaseRemoteConfig by inject()
    private val settings: Settings by inject()
    private val sharedViewModel: MainSharedViewModel by activityViewModels()
    private val storage: FirebaseStorage by inject()
    private var targetPath: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        targetPath = requireContext().getPointerSaveDir()

        requireActivity().apply {
            binding.layoutNewPointer.setOnClickListener {
                showListPointerChooser(pointerType = POINTER_TOUCH)
            }
            binding.layoutNewMouse.setOnClickListener {
                showListPointerChooser(
                    pointerType = POINTER_MOUSE,
                    title = getString(R.string.dialog_title_select_mouse_pointer)
                )
            }
            findViewById<ExtendedFloatingActionButton>(R.id.fab_apply).apply {
                setOnClickListener {
                    applyPointer()
                }
                icon = requireContext().getDrawableExt(R.drawable.ic_action_apply)
            }
            setUpAd()

            loadCurrentPointers()

            binding.actionCustomize.setOnClickListener {
                if (!isPointerSelected()) {
                    sharedViewModel.displayMsg(getString(R.string.msg_pointer_not_selected))
                } else {
                    val bundle = Bundle().apply {
                        putInt("TYPE", POINTER_TOUCH)
                    }
                    val extras =
                        FragmentNavigatorExtras(binding.selectedPointer to getString(R.string.main_fragment_transition))
                    findNavController(R.id.fragment_repo_nav).navigate(R.id.customizeFragment, bundle, null, extras)
                }
            }

            binding.actionCustomizeMouse.setOnClickListener {
                if (!isMouseSelected()) {
                    sharedViewModel.displayMsg(getString(R.string.msg_mouse_not_selected))
                } else {
                    val bundle = Bundle().apply {
                        putInt("TYPE", POINTER_MOUSE)
                    }
                    val extras = FragmentNavigatorExtras(binding.selectedMouse to getString(R.string.transition_mouse))
                    findNavController(R.id.fragment_repo_nav).navigate(R.id.customizeFragment, bundle, null, extras)
                }
            }
        }
    }

    private fun isPointerSelected(): Boolean {
        if (binding.selectedPointer.width == 0 || binding.selectedPointer.height == 0) {
            return false
        }
        return true
    }

    private fun isMouseSelected(): Boolean {
        if (binding.selectedMouse.width == 0 || binding.selectedMouse.height == 0) {
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
                binding.currentPointer.apply {
                    setImageDrawable(Drawable.createFromPath(pointerPath))
                    minimumHeight = requireContext().getMinPointerSizePx()
                    minimumWidth = requireContext().getMinPointerSizePx()
                }
                binding.textNoPointerApplied.visible(false)
            } else {
                binding.textNoPointerApplied.visible(true)
                binding.currentPointer.visible(false)
            }

            if (mousePath != null) {
                binding.currentMouse.setImageDrawable(Drawable.createFromPath(mousePath))
                binding.textNoMouseApplied.visible(false)
            } else {
                binding.textNoMouseApplied.visible(true)
                binding.currentMouse.visible(false)
            }

            if (selectedPointerPath != null) {
                binding.selectedPointer.apply {
                    layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
                    setPadding(padding, padding, padding, padding)
                    setColorFilter(pointerColor)
                    imageAlpha = if (settings.isEnableAlpha) settings.pointerAlpha else 255
                }
                GlideApp.with(requireContext())
                    .load(Uri.fromFile(File(selectedPointerPath)))
                    .into(binding.selectedPointer)
            }
            if (selectedMousePath != null) {
                binding.selectedMouse.apply {
                    layoutParams = FrameLayout.LayoutParams(mouseSize, mouseSize, Gravity.CENTER)
                    setPadding(mousePadding, mousePadding, mousePadding, mousePadding)
                    setColorFilter(mouseColor)
                    imageAlpha = if (settings.isEnableAlpha) settings.mouseAlpha else 255
                }
                GlideApp.with(requireContext())
                    .load(Uri.fromFile(File(selectedMousePath)))
                    .into(binding.selectedMouse)
            }
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun applyPointer() {
        if (!isPointerSelected()) {
            sharedViewModel.displayMsg(getString(R.string.msg_pointer_not_selected))
            return
        }
        if (!isMouseSelected()) {
            sharedViewModel.displayMsg(getString(R.string.msg_mouse_not_selected))
            return
        }
        val filesDir = requireContext().getPointerSaveRootDir()
        val pointerPath = "$filesDir/pointer.png"
        val mousePath = "$filesDir/mouse.png"
        settings.pointerPath = pointerPath
        settings.mousePath = mousePath
        createFileFromView(binding.selectedPointer, pointerPath)
        createFileFromView(binding.selectedMouse, mousePath)

        binding.textNoPointerApplied.visible(false)
        binding.textNoMouseApplied.visible(false)
        binding.currentPointer.apply {
            visible(true)
            setImageDrawable(Drawable.createFromPath(pointerPath))
        }
        binding.currentMouse.apply {
            visible(true)
            setImageDrawable(Drawable.createFromPath(mousePath))
        }

        val reviewManager =
            if (BuildConfig.DEBUG) FakeReviewManager(requireContext()) else ReviewManagerFactory.create(requireContext())
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener {
            if (it.isSuccessful) {
                val reviewInfo = it.result
                val flow = reviewManager.launchReviewFlow(requireActivity(), reviewInfo)
                flow.addOnCompleteListener {
                    showInterstitialAd()
                }
            } else {
                showInterstitialAd()
            }
        }
    }

    private fun showInterstitialAd() {
        interstitialAd.apply {
            if (isLoaded) show() else {
                //TODO test SnackBarMsg data class
                requireActivity().find<CoordinatorLayout>(R.id.container).longSnackbar(
                    message = getString(R.string.text_pointer_applied),
                    actionText = getString(R.string.reboot)
                ) {
                    showRebootDialog()
                }.anchorView = requireActivity().find<BottomNavigationView>(R.id.navigation)
            }
            adListener = object : AdListener() {
                override fun onAdClosed() {
                    super.onAdClosed()
                    interstitialAd.loadAd(AdRequest.Builder().build())
                    requireActivity().find<CoordinatorLayout>(R.id.container).longSnackbar(
                        message = getString(R.string.text_pointer_applied),
                        actionText = getString(R.string.reboot)
                    ) {
                        showRebootDialog()
                    }.anchorView = requireActivity().find<BottomNavigationView>(R.id.navigation)
                }
            }
        }
    }

    private fun showRebootDialog() { //TODO Implement libsuperuser https://github.com/Chainfire/libsuperuser
        MaterialDialog(requireActivity(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
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
                    //TODO Verify ad unit ids
                    adSize = AdSize.BANNER
                    adUnitId = if (BuildConfig.DEBUG || (!it.isSuccessful)) {
                        getString(R.string.ad_banner_unit_id)
                    } else remoteConfig.getString("ad_main_unit_id")
                    binding.adContainer.addView(this)
                    loadAd(AdRequest.Builder().build())
                }

                interstitialAd.apply {
                    adUnitId = if (BuildConfig.DEBUG || (!it.isSuccessful)) {
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
        val bottomSheetListBinding = LayoutListBottomsheetBinding.inflate(layoutInflater)
        val dialog = MaterialDialog(requireContext(), BottomSheet(LayoutMode.MATCH_PARENT)).show {
            customView(view = bottomSheetListBinding.root)
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
                    .into(if (pointerType == POINTER_TOUCH) binding.selectedPointer else binding.selectedMouse)

                dialog.dismiss()
            }

            override fun onLongClick(position: Int, item: RoomPointer): Boolean {
                MaterialDialog(requireActivity(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
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
                return true
            }

        })

        bottomSheetListBinding.listPointers.apply {
            val lm = LinearLayoutManager(requireContext())
            layoutManager = lm
            addItemDecoration(DividerItemDecoration(this.context, lm.orientation))
            this.adapter = pointerAdapter
            scheduleLayoutAnimation()
        }

        lifecycleScope.launch {
            //Observe Db on CoroutineScope
            myDatabase.pointerDao().getAll().observe(viewLifecycleOwner, {
                it.forEach { pointer ->
                    val file = File(requireContext().getPointerSaveDir() + pointer.file_name!!)
                    if (!file.exists()) {
                        Log.d(TAG, "Missing: ${pointer.file_name}")
                        downloadPointer(pointer)
                    }
                }
                pointerAdapter.submitList(it)
                //Show install msg if no pointer installed
                bottomSheetListBinding.apply {
                    infoNoPointerInstalled.visible(it.isEmpty())
                    textDialogHint.visible(it.isNotEmpty())
                    bsButtonInstallPointers.setOnClickListener {
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

    private fun downloadPointer(roomPointer: RoomPointer) {
        val dialog = requireContext().showStaticProgressDialog(getString(R.string.text_progress_downloading_missing))
        lifecycleScope.launch(Dispatchers.IO) {
            val ref = storage.reference.child(DatabaseFields.COLLECTION_POINTERS).child(roomPointer.file_name!!)
            ref.getFile(File("$targetPath${roomPointer.file_name}"))
                .addOnSuccessListener {
                    requireContext().toast(getString(R.string.msg_missing_pointers_auto_downloaded))
                    dialog.dismiss()
                }.addOnFailureListener {
                    requireContext().toast(getString(R.string.msg_error_pointers_missing))
                    dialog.dismiss()
                }
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

    companion object {
        private const val TAG = "MainFragment"
    }
}
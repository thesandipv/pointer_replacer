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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.net.toUri
import androidx.core.view.setPadding
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
import com.afterroot.allusive2.BuildConfig
import com.afterroot.allusive2.Constants.POINTER_MOUSE
import com.afterroot.allusive2.Constants.POINTER_TOUCH
import com.afterroot.allusive2.GlideApp
import com.afterroot.allusive2.R
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.adapter.LocalPointersAdapter
import com.afterroot.allusive2.adapter.callback.ItemSelectedCallback
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.database.MyDatabase
import com.afterroot.allusive2.database.addLocalPointer
import com.afterroot.allusive2.databinding.FragmentMainBinding
import com.afterroot.allusive2.databinding.LayoutListBottomsheetBinding
import com.afterroot.allusive2.getMinPointerSize
import com.afterroot.allusive2.getPointerSaveDir
import com.afterroot.allusive2.getPointerSaveRootDir
import com.afterroot.allusive2.magisk.reboot
import com.afterroot.allusive2.model.RoomPointer
import com.afterroot.allusive2.ui.SplashActivity
import com.afterroot.allusive2.utils.whenBuildIs
import com.afterroot.allusive2.viewmodel.MainSharedViewModel
import com.afterroot.utils.VersionCheck
import com.afterroot.utils.data.model.VersionInfo
import com.afterroot.utils.extensions.getAsBitmap
import com.afterroot.utils.extensions.getDrawableExt
import com.afterroot.utils.extensions.showStaticProgressDialog
import com.afterroot.utils.extensions.visible
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.find
import org.jetbrains.anko.toast
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import com.afterroot.allusive2.resources.R as CommonR

@AndroidEntryPoint
class MainFragment : Fragment() {

    @Inject lateinit var firestore: FirebaseFirestore
    @Inject lateinit var gson: Gson
    @Inject lateinit var myDatabase: MyDatabase
    @Inject lateinit var remoteConfig: FirebaseRemoteConfig
    @Inject lateinit var settings: Settings
    @Inject lateinit var storage: FirebaseStorage
    private lateinit var binding: FragmentMainBinding
    private val sharedViewModel: MainSharedViewModel by viewModels()
    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false
    private var targetPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        setUpVersionCheck()
        targetPath = requireContext().getPointerSaveDir()

        requireActivity().apply {
            binding.layoutNewPointer.setOnClickListener {
                showListPointerChooser(pointerType = POINTER_TOUCH)
            }
            binding.layoutNewMouse.setOnClickListener {
                showListPointerChooser(
                    pointerType = POINTER_MOUSE,
                    title = getString(CommonR.string.dialog_title_select_mouse_pointer)
                )
            }
            findViewById<ExtendedFloatingActionButton>(R.id.fab_apply).apply {
                icon = requireContext().getDrawableExt(CommonR.drawable.ic_action_apply)
                setOnClickListener {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Apply Pointer With")
                        .setItems(CommonR.array.pointer_apply_modes) { _, which ->
                            when (which) {
                                0 -> { // Xposed Method
                                    applyPointer()
                                }
                                1 -> { // Magisk - framework-res Method
                                    if (!isPointerSelected()) {
                                        sharedViewModel.displayMsg(getString(CommonR.string.msg_pointer_not_selected))
                                        return@setItems
                                    }
                                    if (!isMouseSelected()) {
                                        sharedViewModel.displayMsg(getString(CommonR.string.msg_mouse_not_selected))
                                        return@setItems
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
                                    showInterstitialAd {
                                        requireActivity().findNavController(R.id.fragment_repo_nav)
                                            .navigate(R.id.magiskFragment)
                                    }
                                }
                                2 -> { // Magisk - RRO Method
                                    showInterstitialAd {
                                        requireActivity().findNavController(R.id.fragment_repo_nav)
                                            .navigate(R.id.magiskRROFragment)
                                    }
                                }
                            }
                        }.show()
                }
            }
            setUpAd()
            loadInterstitialAd()

            loadCurrentPointers()

            binding.actionCustomize.setOnClickListener {
                if (!isPointerSelected()) {
                    sharedViewModel.displayMsg(getString(CommonR.string.msg_pointer_not_selected))
                } else {
                    val bundle = Bundle().apply {
                        putInt("TYPE", POINTER_TOUCH)
                    }
                    val extras =
                        FragmentNavigatorExtras(binding.selectedPointer to getString(CommonR.string.main_fragment_transition))
                    findNavController(R.id.fragment_repo_nav).navigate(R.id.customizeFragment, bundle, null, extras)
                }
            }

            binding.actionCustomizeMouse.setOnClickListener {
                if (!isMouseSelected()) {
                    sharedViewModel.displayMsg(getString(CommonR.string.msg_mouse_not_selected))
                } else {
                    val bundle = Bundle().apply {
                        putInt("TYPE", POINTER_MOUSE)
                    }
                    val extras = FragmentNavigatorExtras(binding.selectedMouse to getString(CommonR.string.transition_mouse))
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
        val appliedPointerSize = settings.appliedPointerSize
        val appliedPointerPadding = settings.appliedPointerPadding
        val appliedMouseSize = settings.appliedMouseSize
        val appliedMousePadding = settings.appliedMousePadding
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
                    // minimumHeight = requireContext().getMinPointerSizePx()
                    // minimumWidth = requireContext().getMinPointerSizePx()
                    layoutParams = FrameLayout.LayoutParams(appliedPointerSize, appliedPointerSize, Gravity.CENTER)
                    setPadding(appliedPointerPadding)
                }
                binding.textNoPointerApplied.visible(false)
            } else {
                binding.textNoPointerApplied.visible(true)
                binding.currentPointer.visible(false)
            }

            if (mousePath != null) {
                binding.currentMouse.apply {
                    setImageDrawable(Drawable.createFromPath(mousePath))
                    layoutParams = FrameLayout.LayoutParams(appliedMouseSize, appliedMouseSize, Gravity.CENTER)
                    setPadding(appliedMousePadding)
                }
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
            sharedViewModel.displayMsg(getString(CommonR.string.msg_pointer_not_selected))
            return
        }
        if (!isMouseSelected()) {
            sharedViewModel.displayMsg(getString(CommonR.string.msg_mouse_not_selected))
            return
        }
        val filesDir = requireContext().getPointerSaveRootDir()
        val pointerPath = "$filesDir/pointer.png"
        val mousePath = "$filesDir/mouse.png"

        settings.pointerPath = pointerPath
        settings.mousePath = mousePath
        settings.appliedPointerSize = settings.pointerSize
        settings.appliedPointerPadding = settings.pointerPadding
        settings.appliedMouseSize = settings.mouseSize
        settings.appliedMousePadding = settings.mousePadding

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
                    showInterstitialAd {
                        showPointerAppliedMessage()
                    }
                }
            } else {
                showInterstitialAd {
                    showPointerAppliedMessage()
                }
            }
        }
    }

    private fun showInterstitialAd(onAdDismiss: () -> Unit = {}) {
        interstitialAd?.let {
            it.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd()
                    onAdDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    interstitialAd = null
                    onAdDismiss()
                }
            }
            it.show(requireActivity())
        }
    }

    fun showPointerAppliedMessage() {
        requireActivity().find<CoordinatorLayout>(R.id.container).longSnackbar(
            message = getString(CommonR.string.text_pointer_applied),
            actionText = getString(CommonR.string.reboot)
        ) {
            showRebootDialog()
        }.anchorView = requireActivity().find<BottomNavigationView>(R.id.navigation)
    }

    private fun showRebootDialog() {
        MaterialAlertDialogBuilder(requireActivity()).apply {
            setTitle(CommonR.string.reboot)
            setMessage(CommonR.string.text_reboot_confirm)
            setPositiveButton(CommonR.string.reboot) { _, _ ->
                try {
                    reboot()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            setNegativeButton("Later") { dialog, _ ->
                dialog.cancel()
            }
            setNeutralButton(CommonR.string.text_soft_reboot) { _, _ ->
                try { // Also try "killall zygote"
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "busybox killall system_server"))
                    process.waitFor()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun loadInterstitialAd() {
        val interstitialAdUnitId: String = whenBuildIs(
            debug = getString(CommonR.string.ad_interstitial_1_id),
            release = remoteConfig.getString("ad_interstitial_1_id")
        )

        InterstitialAd.load(
            requireContext(),
            interstitialAdUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isAdLoading = false
                    // interstitialAd.show(requireActivity())
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isAdLoading = false
                    super.onAdFailedToLoad(loadAdError)
                }
            }
        )
    }

    private fun setUpAd() {
        sharedViewModel.savedStateHandle.getLiveData<Boolean>("configLoaded").observe(requireActivity()) {
            if (!it) return@observe
            kotlin.runCatching {
                val bannerAdUnitId: String = whenBuildIs(
                    debug = getString(CommonR.string.ad_banner_unit_id),
                    release = remoteConfig.getString("ad_main_unit_id")
                )

                val adView = AdView(requireContext())
                adView.apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = bannerAdUnitId
                    binding.adContainer.addView(this)
                    loadAd(AdRequest.Builder().build())
                }
            }
        }
    }

    private fun setUpVersionCheck() {
        sharedViewModel.savedStateHandle.getLiveData<Boolean>("configLoaded").observe(requireActivity()) {
            if (!it) return@observe
            val versionJson = remoteConfig.getString("versions_allusive")
            if (versionJson.isBlank()) return@observe
            val versionChecker = VersionCheck(
                gson.fromJson(versionJson, VersionInfo::class.java)
                    .copy(currentVersion = BuildConfig.VERSION_CODE)
            )
            versionChecker.onVersionDisabled {
                AlertDialog.Builder(requireContext()).apply {
                    setTitle("Version Obsolete")
                    setMessage("This version is obsolete. You have to update to latest version.")
                    setPositiveButton("Update") { _, _ ->
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")
                        )
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }
                    setNegativeButton(android.R.string.cancel) { _, _ ->
                        requireActivity().finish()
                    }
                    setCancelable(false)
                }.show()
            }
            versionChecker.onUpdateAvailable {
                AlertDialog.Builder(requireContext()).apply {
                    setTitle("Update Available")
                    setMessage("New Version Available. Please update to get latest features.")
                    setPositiveButton("Update") { _, _ ->
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")
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

    private fun generateRoomPointerFromFileName(fileNames: List<String>) {
        fileNames.forEach { filename ->
            lifecycleScope.launch {
                myDatabase.addLocalPointer(filename)
            }
        }
    }

/*
    private fun addLocalPointer(fileName: String) {
        val pointer = RoomPointer(
            file_name = fileName,
            uploader_name = "You (Local)",
            uploader_id = "N/A",
            pointer_name = fileName,
            pointer_desc = "N/A"
        )
        lifecycleScope.launch {
            addRoomPointer(pointer)
        }
    }
*/

/*
    private suspend fun addRoomPointer(roomPointer: RoomPointer) {
        if (roomPointer.file_name == null) {
            return
        }
        if (myDatabase.pointerDao().exists(roomPointer.file_name!!).isEmpty()) {
            myDatabase.pointerDao().add(roomPointer)
        }
    }
*/

    private fun import() {
        val safUri = settings.safUri ?: ""
        when {
            settings.safUri == "" -> {
                Timber.d("import: Pointer Folder Uri not found. Asking for permission.")
                // runImport = true
                askForPointerFolderLocation()
            }
            arePermissionsGranted(safUri) -> {
                // DO WORK
                val fileNames = arrayListOf<String>()
                val pointerFolder = DocumentFile.fromTreeUri(requireContext(), settings.safUri!!.toUri())

                pointerFolder?.listFiles()?.filterNotNull()?.forEach {
                    it.name?.let { fileName ->
                        if (fileName == ".nomedia") return
                        fileNames.add(fileName)
                        lifecycleScope.launch(Dispatchers.IO) {
                            requireContext().contentResolver.openInputStream(it.uri)?.use { fis ->
                                val saveFile = File(requireContext().getPointerSaveDir(), fileName)
                                FileOutputStream(saveFile).use { fos ->
                                    fis.copyTo(fos)
                                }
                            }
                        }
                    }
                }
                lifecycleScope.launch {
                    generateRoomPointerFromFileName(fileNames)
                }
            }
            else -> {
                Timber.d("import: Pointer Folder Uri permission not stored. Asking for permission.")
                // runImport = true
                askForPointerFolderLocation()
            }
        }
    }

    private fun arePermissionsGranted(uriString: String): Boolean {
        // list of all persisted permissions for our app
        val list = requireActivity().contentResolver.persistedUriPermissions
        for (i in list.indices) {
            val persistedUriString = list[i].uri.toString()
            if (persistedUriString == uriString && list[i].isWritePermission && list[i].isReadPermission) {
                return true
            }
        }
        return false
    }

    private val openTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        Timber.d("Selected Uri: %s", it.toString())
        if (it == null) return@registerForActivityResult
        val dir = DocumentFile.fromTreeUri(requireContext(), it)
        if (dir?.findFile(".nomedia")?.exists() == false) {
            dir.createFile("*/text", ".nomedia")
        }
        settings.safUri = it.toString()

        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        requireActivity().contentResolver.takePersistableUriPermission(it, takeFlags)

        Toast.makeText(requireContext(), "Selected: ${it.path}", Toast.LENGTH_SHORT).show()
    }

    private fun askForPointerFolderLocation() {
        openTree.launch(null)
    }

    private lateinit var pointerAdapter: LocalPointersAdapter
    private fun showListPointerChooser(
        title: String = getString(CommonR.string.dialog_title_select_pointer),
        pointerType: Int
    ) {
        val bottomSheetListBinding = LayoutListBottomsheetBinding.inflate(layoutInflater)
        val dialog = MaterialDialog(requireContext(), BottomSheet(LayoutMode.MATCH_PARENT)).show {
            customView(view = bottomSheetListBinding.root)
            title(text = title)
            noAutoDismiss()
            positiveButton(text = "Import Your Pointers") { // Remove this
                try {
                    import()
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
            }
        }

        pointerAdapter = LocalPointersAdapter(object : ItemSelectedCallback<RoomPointer> {
            override fun onClick(position: Int, view: View?, item: RoomPointer) {
                if (pointerType == POINTER_TOUCH) {
                    settings.selectedPointerName = item.pointer_name
                    settings.selectedPointerPath = targetPath + item.file_name
                } else {
                    settings.selectedMouseName = item.pointer_name
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
                    title(text = "${getString(CommonR.string.text_delete)} ${item.pointer_name}")
                    message(res = CommonR.string.text_delete_confirm)
                    positiveButton(res = CommonR.string.text_yes) {
                        val pointerFile = File(targetPath + item.file_name)
                        if (!pointerFile.exists() || pointerFile.delete()) {
                            lifecycleScope.launch {
                                myDatabase.pointerDao().delete(item)
                            }
                            Toast.makeText(context, getString(CommonR.string.msg_delete_success), Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            Toast.makeText(context, getString(CommonR.string.msg_delete_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                    negativeButton(res = CommonR.string.text_no)
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
            // Observe Db on CoroutineScope
            myDatabase.pointerDao().getAll().observe(viewLifecycleOwner) {
                it.forEach { pointer ->
                    val file = File(requireContext().getPointerSaveDir() + pointer.file_name!!)
                    if (!file.exists()) {
                        Timber.tag(TAG).d("Missing: %s", pointer.file_name)
                        // downloadPointer(pointer)
                    }
                }
                pointerAdapter.submitList(it)
                // Show install msg if no pointer installed
                bottomSheetListBinding.apply {
                    infoNoPointerInstalled.visible(it.isEmpty())
                    textDialogHint.visible(it.isNotEmpty())
                    bsButtonInstallPointers.setOnClickListener {
                        dialog.dismiss()
                        requireActivity().findNavController(R.id.fragment_repo_nav)
                            .navigate(R.id.repoFragment)
                    }
                    bsButtonImportPointers.setOnClickListener {
                        import()
                    }
                }
            }
        }
    }

    private fun downloadPointer(roomPointer: RoomPointer) {
        val dialog = requireContext().showStaticProgressDialog(getString(CommonR.string.text_progress_downloading_missing))
        lifecycleScope.launch(Dispatchers.IO) {
            val ref = storage.reference.child(DatabaseFields.COLLECTION_POINTERS).child(roomPointer.file_name!!)
            ref.getFile(File("$targetPath${roomPointer.file_name}"))
                .addOnSuccessListener {
                    requireContext().toast(getString(CommonR.string.msg_missing_pointers_auto_downloaded))
                    dialog.dismiss()
                }.addOnFailureListener {
                    requireContext().toast(getString(CommonR.string.msg_error_pointers_missing))
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
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(CommonR.string.dialog_title_sign_out))
            .setMessage(getString(CommonR.string.dialog_msg_sign_out))
            .setPositiveButton(CommonR.string.dialog_title_sign_out) { _, _ ->
                AuthUI.getInstance().signOut(requireContext()).addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        getString(CommonR.string.dialog_sign_out_result_success),
                        Toast.LENGTH_SHORT
                    ).show()
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

/*
 * Copyright (C) 2016-2022 Sandip Vaghela
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
package com.afterroot.allusive2.magisk

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.scale
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.afterroot.allusive2.Result
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.magisk.databinding.FragmentMagiskBinding
import com.afterroot.core.extensions.getAsBitmap
import com.afterroot.core.extensions.visible
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class MagiskRROFragment : Fragment() {

    private lateinit var binding: FragmentMagiskBinding
    @Inject lateinit var settings: Settings
    private val progress = MutableLiveData<Result>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMagiskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        progress.observe(viewLifecycleOwner) {
            when (it) {
                is Result.Failed -> {
                }
                is Result.Running -> {
                    binding.progressBar.visible(true)
                    binding.message = it.message
                }
                Result.Success -> {
                    binding.progressBar.visible(false)
                }
            }
        }

        // Fake Update one time
        updateProgress()

        binding.openMagisk.apply {
            visible(false)
            setOnClickListener {
                val intent = requireContext().packageManager.getLaunchIntentForPackage(MAGISK_PACKAGE)
                if (intent != null) {
                    startActivity(intent)
                } else Toast.makeText(requireContext(), "Magisk Manager not Installed", Toast.LENGTH_SHORT).show()
            }
        }

        setPointerImage()

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            updateProgress("- Android 11 and Up Not Supported")
            updateProgress(completed = true)
            return
        }*/

        val selectedPointerModule =
            File(repackedMagiskModulePath(requireContext(), "${settings.selectedPointerName}_RRO_Magisk.zip"))
        if (selectedPointerModule.exists()) {
            setupInstallButton(selectedPointerModule.path)
            updateProgress("- Magisk module already exist at: ${selectedPointerModule.path}")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Magisk module exist")
                .setMessage(
                    """- Magisk module already exist at: ${selectedPointerModule.path}
                        |- If you changed pointer size click 'REPACK ANYWAY' to repack Magisk Module.
                        |- If you want to repack anyway click 'REPACK ANYWAY'""".trimMargin()
                )
                .setPositiveButton("REPACK ANYWAY") { _, _ ->
                    setupInstallButton(selectedPointerModule.path, false)
                    // createMagiskModule()
                    listRROApks()
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                }
                .show()
            updateProgress(completed = true)
            return
        }
        listRROApks()
        // createMagiskModule()
    }

    private fun setPointerImage() {
        if (settings.selectedPointerPath != null) {
            binding.pointerContainer.visible(true)
            binding.ivCurrentPointer.apply {
                visible(true)
                setPadding(settings.appliedPointerPadding)
                setColorFilter(settings.pointerColor)
                imageAlpha = if (settings.isEnableAlpha) settings.pointerAlpha else 255
                Glide.with(requireContext())
                    .load(Uri.fromFile(File(settings.selectedPointerPath!!)))
                    .override(128)
                    .into(this)
            }
        }
    }

    private fun createMagiskModule(rroApk: File) {
        lifecycleScope.launch {
            copyAndExtractMagiskRROModuleZip()
            copySelectedRROFile(rroApk)
            delay(500)
            createAndReplacePointerFiles(ALL_VARIANTS)
            repackAllusiveRROApk()
            copyRepackedRROApk(rroApk.name)

            val module = repackMagiskModuleZip()
            if (module?.exists() == true) {
                updateProgress("- Magisk module saved at: ${module.path}")
                setupInstallButton(module.path)
            }
            updateProgress(completed = true)
        }
    }

    private fun setupInstallButton(path: String, visible: Boolean = true) {
        binding.installModule.apply {
            visible(visible)
            setOnClickListener {
                showRROExperimentalWarning(requireContext()) { response ->
                    if (!response) return@showRROExperimentalWarning
                    installModule(path) {
                        it.out.forEach { output ->
                            updateProgress(output)
                        }
                        it.err.forEach { error ->
                            updateProgress(error)
                        }
                        if (it.isSuccess) {
                            updateProgress(completed = true)
                            showRebootDialog(requireContext())
                        } else {
                            updateProgress("- Module installation failed")
                            updateProgress(completed = true)
                        }
                    }
                }
            }
        }
    }

    private fun updateProgress(progressText: String = "", completed: Boolean = false) {
        val stringBuilder = StringBuilder()
        if (progress.value is Result.Running) {
            val oldResult = (progress.value as Result.Running).message
            stringBuilder.append(oldResult)
            stringBuilder.appendLine(progressText)
        }

        if (completed) {
            // Wait before sending result
            lifecycleScope.launch {
                delay(300)
                progress.value = Result.Success
            }
            return
        }
        progress.value = Result.Running(stringBuilder.toString())
    }

    /**
     * DONE
     */
    private suspend fun copyAndExtractMagiskRROModuleZip() {
        withContext(Dispatchers.Main) { updateProgress("- Copying rro-module.zip from assets") }
        withContext(Dispatchers.IO) {
            copyMagiskRROZip()
            withContext(Dispatchers.Main) { updateProgress("- Extracting rro-module.zip") }
            File(magiskRROModuleZipPath(requireContext()))
                .unzip(toFolder = File(magiskRROModuleExtractPath(requireContext())))

            // Delete placeholder file if exists
            File("${magiskRROModuleExtractPath(requireContext())}/system/vendor/overlay/placeholder").apply {
                if (exists() && delete()) {
                    withContext(Dispatchers.Main) { updateProgress("- Deleted Placeholder file") }
                }
            }

            withContext(Dispatchers.Main) { updateProgress("- Done Extracting") }
        }
    }

    private fun listRROApks() {
        val files = File("system/vendor/overlay").listFiles()
        var initialSelectionIndex = 0

        if (files == null) {
            Timber.d("listRROApks: no overlay files found.")
            return
        }

        Timber.d("listRROApks: ${files.size}")
        val fileNames = Array(files.size) {
            files[it].name
        }

        files.forEachIndexed { index, file ->
            if (file.name == "framework-res__auto_generated_rro_vendor.apk") {
                initialSelectionIndex = index
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select app to modify")
            .setSingleChoiceItems(
                fileNames,
                initialSelectionIndex
            ) { dialog, which ->
                createMagiskModule(files[which])
                dialog.dismiss()
            }.show()
    }

    private suspend fun copySelectedRROFile(rroApk: File) {
        withContext(Dispatchers.IO) {
            withContext(Dispatchers.Main) { updateProgress("- Extracting ${rroApk.name}") }

            rroApk.unzip(toFolder = File(magiskRROApkExtractPath(requireContext())))

            withContext(Dispatchers.Main) { updateProgress("- Done Extracting") }
        }
    }

    /**
     * DONE
     */
    private suspend fun createAndReplacePointerFiles(variants: List<Variant>) {
        val mdpiPointer = "${magiskRROApkExtractPath(requireContext())}$POINTER_MDPI"
        val hdpiPointer = "${magiskRROApkExtractPath(requireContext())}$POINTER_HDPI"
        val xhdpiPointer = "${magiskRROApkExtractPath(requireContext())}$POINTER_XHDPI"
        withContext(Dispatchers.IO) {
            File(mdpiPointer).parentFile?.mkdirs() ?: throw IOException("Failed to create directory")
            File(hdpiPointer).parentFile?.mkdirs() ?: throw IOException("Failed to create directory")
            File(xhdpiPointer).parentFile?.mkdirs() ?: throw IOException("Failed to create directory")
        }
        withContext(Dispatchers.Main) {
            updateProgress("- Selected Pointer: ${settings.selectedPointerName}")

            val bmp: Bitmap = binding.ivCurrentPointer.getAsBitmap() ?: return@withContext

            variants.forEach { variant ->
                when (variant) {
                    Variant.MDPI -> {
                        val scaled = bmp.scale(VariantSizes.MDPI, VariantSizes.MDPI)
                        scaled.saveAs(mdpiPointer).apply {
                            if (this.exists()) updateProgress("- Replaced MDPI pointer_spot_touch.png")
                        }
                    }
                    Variant.HDPI -> {
                        val scaled = bmp.scale(VariantSizes.HDPI, VariantSizes.HDPI)
                        scaled.saveAs(hdpiPointer).apply {
                            if (this.exists()) updateProgress("- Replaced HDPI pointer_spot_touch.png")
                        }
                    }
                    Variant.XHDPI -> {
                        val scaled = bmp.scale(VariantSizes.XHDPI, VariantSizes.XHDPI)
                        scaled.saveAs(xhdpiPointer).apply {
                            if (this.exists()) updateProgress("- Replaced XHDPI pointer_spot_touch.png")
                        }
                    }
                }
            }
        }
    }

    /**
     * DONE
     */
    private suspend fun repackAllusiveRROApk(): File? {
        var result: File?
        updateProgress("- Repacking allusive_rro.apk")
        withContext(Dispatchers.IO) {
            val path = magiskRROApkExtractPath(requireContext())
            result = zip(sourceFolder = File(path), exportPath = repackedRROApkPath(requireContext()))
        }
        updateProgress("- Repack Successful")
        return result
    }

    /**
     * DONE
     */
    private suspend fun repackMagiskModuleZip(): File? {
        var result: File?
        updateProgress("- Repacking magisk module")
        withContext(Dispatchers.IO) {
            val path = magiskRROModuleExtractPath(requireContext())
            val fileName = "${settings.selectedPointerName}_RRO_Magisk.zip"
            result = zip(sourceFolder = File(path), exportPath = repackedMagiskModulePath(requireContext(), fileName))
        }
        updateProgress("- Repack Successful")
        return result
    }

    /**
     * DONE
     */
    private suspend fun copyMagiskRROZip() {
        withContext(Dispatchers.Main) {
            updateProgress("- Copying $MAGISK_RRO_ZIP")
        }
        withContext(Dispatchers.IO) {
            copyMagiskRROZip(context = requireContext(), to = magiskRROModuleZipPath(requireContext()))
        }
        withContext(Dispatchers.Main) {
            updateProgress("- Done copying $MAGISK_RRO_ZIP")
        }
    }

    /**
     * DONE
     */
    private suspend fun extractMagiskRROZip() {
        updateProgress("- Extracting $MAGISK_RRO_ZIP")
        withContext(Dispatchers.IO) {
            extractMagiskZip(requireContext())
        }
        updateProgress("- Done Extracting $MAGISK_RRO_ZIP")
    }

    /**
     * DONE
     */
    private suspend fun copyRepackedRROApk(repackName: String): File {
        updateProgress("- Copying repacked $repackName")
        val result = withContext(Dispatchers.IO) {
            copyRepackedRROApk(requireContext(), repackName)
        }
        updateProgress("- Done copying repacked $repackName")
        return result
    }
}

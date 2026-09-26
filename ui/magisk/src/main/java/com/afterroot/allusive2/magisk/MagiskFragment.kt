/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.magisk

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.scale
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.afterroot.allusive2.Result
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.magisk.databinding.FragmentMagiskBinding
import com.afterroot.utils.extensions.visible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile

@AndroidEntryPoint
class MagiskFragment : Fragment() {

  private lateinit var binding: FragmentMagiskBinding

  @Inject lateinit var settings: Settings
  private val progress = MutableLiveData<Result>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
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
        val intent = requireContext().packageManager.getLaunchIntentForPackage(
          MAGISK_PACKAGE,
        )
        if (intent != null) {
          startActivity(intent)
        } else {
          Toast.makeText(
            requireContext(),
            "Magisk Manager not Installed",
            Toast.LENGTH_SHORT,
          ).show()
        }
      }
    }

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            updateProgress("- Android 11 and Up Not Supported")
            updateProgress(completed = true)
            return
        }*/

    val selectedPointerModule =
      File(
        repackedMagiskModulePath(
          requireContext(),
          "${settings.selectedPointerName}_Magisk.zip",
        ),
      )
    if (selectedPointerModule.exists()) {
      setupInstallButton(selectedPointerModule.path)
      updateProgress("- Magisk module already exist at: ${selectedPointerModule.path}")
      MaterialAlertDialogBuilder(requireContext())
        .setTitle("Magisk module exist")
        .setMessage(
          """- Magisk module already exist at: ${selectedPointerModule.path}
                        |- If you changed pointer size click 'Yes' to repack Magisk Module.
                        |- If you want to repack anyway click 'Yes'
          """.trimMargin(),
        )
        .setPositiveButton("REPACK ANYWAY") { _, _ ->
          setupInstallButton(selectedPointerModule.path, false)
          createMagiskModule()
        }
        .setNegativeButton(android.R.string.cancel) { _, _ ->
        }
        .show()
      updateProgress(completed = true)
      return
    }
    createMagiskModule()
  }

  private fun createMagiskModule() {
    lifecycleScope.launch {
      createAndReplacePointerFiles(ALL_VARIANTS)
      updateProgress("- Modifying framework-res.apk with new pointer...")
      val repackedFw = withContext(Dispatchers.IO) {
        repackFrameworkResApk()
      }
      if (repackedFw == null || !repackedFw.exists()) {
        updateProgress("- Failed to repack framework-res.apk", completed = true)
        return@launch
      }

      updateProgress("- Packaging Magisk Module...")
      val module = withContext(Dispatchers.IO) {
        val fileName = "${settings.selectedPointerName}_Magisk.zip"
        buildFrameworkMagiskModule(
          context = requireContext(),
          pointerName = settings.selectedPointerName ?: "Custom",
          repackedFrameworkApk = repackedFw,
          outputPath = repackedMagiskModulePath(requireContext(), fileName),
        )
      }

      if (module.exists()) {
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
        installModule(
          path = path,
          callback = {
            it.err.forEach { error ->
              updateProgress("Error: $error")
            }
            if (it.isSuccess) {
              updateProgress(completed = true)
              showRebootDialog(requireContext())
            } else {
              updateProgress("- Module installation failed")
              updateProgress(completed = true)
            }
          },
          onElementAdd = { element ->
            element?.let { it -> updateProgress(it) }
          },
        )
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
      Handler(Looper.getMainLooper()).postDelayed(
        {
          progress.value = Result.Success
        },
        300,
      )

      return
    }
    progress.value = Result.Running(stringBuilder.toString())
  }

  private fun createAndReplacePointerFiles(variants: List<Variant>) {
    val pointersDir = File(pointerSavePath(requireContext()))
    if (!pointersDir.exists()) pointersDir.mkdirs()

    val selectedPointer = settings.pointerPath ?: return
    updateProgress("- Selected Pointer: ${settings.selectedPointerName}")

    val bmp: Bitmap = BitmapFactory.decodeFile(selectedPointer)

    variants.forEach { variant ->
      when (variant) {
        Variant.MDPI -> {
          val scaled = bmp.scale(VariantSizes.MDPI, VariantSizes.MDPI)
          scaled.saveAs("${pointerSavePath(requireContext())}$POINTER_MDPI").apply {
            if (this.exists()) updateProgress("- Replaced MDPI pointer_spot_touch.png")
          }
        }
        Variant.HDPI -> {
          val scaled = bmp.scale(VariantSizes.HDPI, VariantSizes.HDPI)
          scaled.saveAs("${pointerSavePath(requireContext())}$POINTER_HDPI").apply {
            if (this.exists()) updateProgress("- Replaced HDPI pointer_spot_touch.png")
          }
        }
        Variant.XHDPI -> {
          val scaled = bmp.scale(VariantSizes.XHDPI, VariantSizes.XHDPI)
          scaled.saveAs("${pointerSavePath(requireContext())}$POINTER_XHDPI").apply {
            if (this.exists()) updateProgress("- Replaced XHDPI pointer_spot_touch.png")
          }
        }
        Variant.XXHDPI -> {
          val scaled = bmp.scale(VariantSizes.XXHDPI, VariantSizes.XXHDPI)
          scaled.saveAs("${pointerSavePath(requireContext())}$POINTER_XXHDPI").apply {
            if (this.exists()) updateProgress("- Replaced XXHDPI pointer_spot_touch.png")
          }
        }
        Variant.XXXHDPI -> {
          val scaled = bmp.scale(VariantSizes.XXXHDPI, VariantSizes.XXXHDPI)
          scaled.saveAs("${pointerSavePath(requireContext())}$POINTER_XXXHDPI").apply {
            if (this.exists()) updateProgress("- Replaced XXXHDPI pointer_spot_touch.png")
          }
        }
      }
    }
  }

  private suspend fun repackFrameworkResApk(): File? {
    var result: File?
    updateProgress("- Repacking framework-res.apk")
    withContext(Dispatchers.IO) {
      File(
        FRAMEWORK_APK,
      ).copyTo(File(repackedFrameworkPath(requireContext())), overwrite = true)
      ZipFile(File(repackedFrameworkPath(requireContext())))
        .addFolder(File("${pointerSavePath(requireContext())}/res"))

      result = File(repackedFrameworkPath(requireContext()))
    }
    updateProgress("- Repack Successful")
    return result
  }
}

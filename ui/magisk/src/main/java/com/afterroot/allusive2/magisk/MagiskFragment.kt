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
            /*val extracted = File(frameworkExtractPath(requireContext()))
            if (!extracted.exists()) {
                copyAndExtractFrameworkResApk()
            } else {
                updateProgress("- Using already extracted framework-res.apk")
            }*/

      createAndReplacePointerFiles(ALL_VARIANTS)
      repackFrameworkResApk()
      copyMagiskModuleZip()
      extractMagiskModuleZip()
      copyRepackedFW()
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

  private suspend fun copyAndExtractFrameworkResApk() {
    updateProgress("- Copying framework-res.apk from /system/framework")
    withContext(Dispatchers.IO) {
      val frameworkResApk = copyFrameworkRes(requireContext())
      withContext(Dispatchers.Main) { updateProgress("- Extracting framework-res.apk") }

      frameworkResApk.unzip(toFolder = File(frameworkExtractPath(requireContext())))

      withContext(Dispatchers.Main) { updateProgress("- Done Extracting") }
    }
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

  private suspend fun repackMagiskModuleZip(): File? {
    var result: File?
    updateProgress("- Repacking magisk module")
    withContext(Dispatchers.IO) {
      val path = magiskEmptyModuleExtractPath(requireContext())
      val fileName = "${settings.selectedPointerName}_Magisk.zip"
      result =
        zip(
          sourceFolder = File(path),
          exportPath = repackedMagiskModulePath(requireContext(), fileName),
        )
    }
    updateProgress("- Repack Successful")
    return result
  }

  private suspend fun copyMagiskModuleZip() {
    updateProgress("- Copying $MAGISK_EMPTY_ZIP")
    withContext(Dispatchers.IO) {
      copyMagiskEmptyZip(
        context = requireContext(),
        to = magiskEmptyModuleZipPath(requireContext()),
      )
    }
    updateProgress("- Done copying $MAGISK_EMPTY_ZIP")
  }

  private suspend fun extractMagiskModuleZip() {
    updateProgress("- Extracting $MAGISK_EMPTY_ZIP")
    withContext(Dispatchers.IO) {
      extractMagiskZip(requireContext())
    }
    updateProgress("- Done Extracting $MAGISK_EMPTY_ZIP")
  }

  private suspend fun copyRepackedFW(): File {
    updateProgress("- Copying repacked framework-res.apk")
    val result = withContext(Dispatchers.IO) {
      copyRepackedFrameworkResApk(requireContext())
    }
    updateProgress("- Done copying framework-res.apk")
    return result
  }
}

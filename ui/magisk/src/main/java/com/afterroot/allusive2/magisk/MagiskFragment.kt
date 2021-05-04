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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.scale
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.magisk.databinding.FragmentMagiskBinding
import com.afterroot.core.extensions.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.File

sealed class Result {
    object Success : Result()
    data class Running(val message: String) : Result()
    data class Failed(val error: String) : Result()
}

class MagiskFragment : Fragment() {

    private lateinit var binding: FragmentMagiskBinding
    private val settings: Settings by inject()
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

        lifecycleScope.launch {
            val extracted = File(frameworkExtractPath(requireContext()))
            if (!extracted.exists()) {
                copyAndExtractFrameworkResApk()
            } else {
                updateProgress("")
                updateProgress("Using already extracted framework-res.apk")
            }

            createAndReplacePointerFiles(variantsToReplace(requireContext()))
            repackFrameworkResApk()
            delay(300)
            updateProgress("Done", true)
        }
    }

    private fun updateProgress(progressText: String, completed: Boolean = false) {
        val stringBuilder = StringBuilder()
        if (progress.value is Result.Running) {
            val oldResult = (progress.value as Result.Running).message
            stringBuilder.append(oldResult)
            stringBuilder.appendLine(progressText)
        }

        if (completed) {
            progress.value = Result.Success
            return
        }
        progress.value = Result.Running(stringBuilder.toString())
    }


    private suspend fun copyAndExtractFrameworkResApk() {
        withContext(Dispatchers.IO) {
            updateProgress("Copying framework-res.apk from /system/framework")
            val frameworkResApk = copyFrameworkRes(requireContext())

            updateProgress("Extracting framework-res.apk")

            frameworkResApk.unzip(File(frameworkExtractPath(requireContext())))

            updateProgress("Done Extracting")
        }
    }

    private fun createAndReplacePointerFiles(variants: List<Variant>) {
        val selectedPointer = settings.selectedPointerPath ?: return
        updateProgress("Selected Pointer: ${settings.selectedPointerName}")

        val bmp: Bitmap = BitmapFactory.decodeFile(selectedPointer)

        variants.forEach { variant ->
            when (variant) {
                Variant.MDPI -> {
                    val scaled = bmp.scale(VariantSizes.MDPI, VariantSizes.MDPI)
                    scaled.saveAs("${frameworkExtractPath(requireContext())}$POINTER_MDPI").apply {
                        if (this.exists()) updateProgress("Replaced MDPI pointer_spot_touch.png")
                    }

                }
                Variant.HDPI -> {
                    val scaled = bmp.scale(VariantSizes.HDPI, VariantSizes.HDPI)
                    scaled.saveAs("${frameworkExtractPath(requireContext())}$POINTER_HDPI").apply {
                        if (this.exists()) updateProgress("Replaced HDPI pointer_spot_touch.png")
                    }
                }
                Variant.XHDPI -> {
                    val scaled = bmp.scale(VariantSizes.XHDPI, VariantSizes.XHDPI)
                    scaled.saveAs("${frameworkExtractPath(requireContext())}$POINTER_XHDPI").apply {
                        if (this.exists()) updateProgress("Replaced XHDPI pointer_spot_touch.png")
                    }
                }
            }
        }
    }

    private suspend fun repackFrameworkResApk(): File? {
        var result: File?
        updateProgress("Repacking framework-res.apk")
        withContext(Dispatchers.IO) {
            val path = frameworkExtractPath(requireContext()) + "/assets"
            val file = File(path)
            result = zip(file, requireContext().externalCacheDir?.path + "/repacked.apk")
        }
        updateProgress("Repack Successful")
        return result
    }

    companion object {
        private const val TAG = "MagiskFragment"
    }
}
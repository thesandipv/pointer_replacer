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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.afterroot.allusive2.Result
import com.afterroot.allusive2.data.pointers
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.magisk.databinding.FragmentMagiskBinding
import com.afterroot.allusive2.model.Pointer
import com.afterroot.utils.extensions.visible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MagiskRROFragment : Fragment() {

    private lateinit var binding: FragmentMagiskBinding
    @Inject lateinit var storage: FirebaseStorage
    @Inject lateinit var firestore: FirebaseFirestore
    @Inject lateinit var okHttpClient: OkHttpClient
    private val progress = MutableLiveData<Result>()
    private lateinit var repoDocId: String
    private lateinit var pointerFileName: String
    private lateinit var selectedPointer: Pointer
    private lateinit var downloadApkFileName: String
    private lateinit var magiskModuleSaveName: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMagiskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repoDocId = arguments?.getString("repoDocId") ?: ""
        pointerFileName = arguments?.getString("pointerFileName") ?: ""
        downloadApkFileName = "RRO_${pointerFileName.substringBeforeLast(".")}.apk"

        lifecycleScope.launch {
            selectedPointer =
                firestore.pointers().document(repoDocId).get(Source.CACHE).await().toObject(Pointer::class.java)
                    ?: firestore.pointers().document(repoDocId).get().await().toObject(Pointer::class.java) ?: Pointer()
            magiskModuleSaveName = "${selectedPointer.name}_RRO-2_Magisk.zip"
            init()
        }
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
        val selectedPointerModule =
            File(repackedMagiskModulePath(requireContext(), magiskModuleSaveName))
        if (selectedPointerModule.exists()) {
            setupInstallButton(selectedPointerModule.path)
            updateProgress("- Magisk module already exist at: ${selectedPointerModule.path}")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Magisk module exist")
                .setMessage(
                    """- Magisk module already exist at: ${selectedPointerModule.path}""".trimMargin()
                )
                .setPositiveButton("OK") { _, _ ->
                    // setupInstallButton(selectedPointerModule.path, false)
                    // createMagiskModule()
                }
                .show()
            updateProgress(completed = true)
            return
        }
        createMagiskModule()
    }

    private fun setPointerImage() {
        binding.pointerContainer.visible(true)
        binding.ivCurrentPointer.apply {
            visible(true)
            val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
            Glide.with(requireContext())
                .load(storage.pointers().child(pointerFileName))
                .override(128)
                .transition(DrawableTransitionOptions.withCrossFade(factory))
                .into(this)
        }
    }

    private fun createMagiskModule() {
        lifecycleScope.launch {
            downloadRROApk()
            copyAndExtractMagiskRROModuleZip()
            copyDownloadedRROApk()

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
                    installModule(
                        path = path,
                        callback = {
                            Timber.d("MagiskInstall: $it")
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
                        }
                    )
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

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun downloadRROApk(): Boolean {
        var result = false
        val url =
            "https://github.com/afterroot/allusive-repo/raw/main/rros/$downloadApkFileName"

        val fileName = downloadApkFileName
        val rroApk = File(rroApkDownloadPath(requireContext()), fileName)

        if (rroApk.parentFile?.exists() != true) {
            rroApk.parentFile?.mkdirs()
        }

        if (rroApk.exists()) {
            updateProgress("- RRO Apk already exist at: ${rroApk.path}")
            return true
        }

        updateProgress("- Downloading RRO Apk from: $url")
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body.byteStream()
                val inputStream = body.buffered()
                val outputStream = rroApk.outputStream()
                inputStream.copyTo(outputStream)
                withContext(Dispatchers.Main) {
                    updateProgress("- RRO Apk saved at: ${rroApk.path}")
                    result = true
                }

                if (repoDocId.isNotBlank()) {
                    firestore.pointers().document(repoDocId)
                        .update(DatabaseFields.FIELD_RRO_DOWNLOADS, FieldValue.increment(1))
                }
            } else {
                withContext(Dispatchers.Main) {
                    updateProgress("- RRO Apk download failed")
                    result = false
                }
            }
        }
        return result
    }

    private suspend fun copyAndExtractMagiskRROModuleZip() {
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

    private suspend fun repackMagiskModuleZip(): File? {
        var result: File?
        updateProgress("- Repacking magisk module")
        withContext(Dispatchers.IO) {
            val path = magiskRROModuleExtractPath(requireContext())
            result = zip(sourceFolder = File(path), exportPath = repackedMagiskModulePath(requireContext(), magiskModuleSaveName))
        }
        updateProgress("- Repack Successful")
        return result
    }

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

    private suspend fun copyDownloadedRROApk(): File {
        updateProgress("- Copying downloaded rro apk")
        val result = withContext(Dispatchers.IO) {
            copyDownloadedRROApk(requireContext(), downloadApkFileName)
        }
        updateProgress("- Done copying downloaded rro apk")
        return result
    }
}

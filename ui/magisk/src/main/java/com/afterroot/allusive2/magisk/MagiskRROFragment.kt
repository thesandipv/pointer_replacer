/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.magisk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.afterroot.allusive2.Constants
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.base.BuildConfig
import com.afterroot.allusive2.data.pointers
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.model.Pointer
import com.afterroot.ui.common.compose.theme.Theme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import com.afterroot.allusive2.resources.R as CommonR

data class MagiskRROUiState(
  val pointerName: String = "",
  val pointerUrl: String? = null,
  val pointerType: Int = Constants.POINTER_TOUCH,
  val logText: String = "",
  val isLoading: Boolean = false,
  val isModuleReady: Boolean = false,
  val modulePath: String? = null,
  val rootManagerName: String = "Root Manager",
)

@AndroidEntryPoint
class MagiskRROFragment : Fragment() {

  @Inject lateinit var storage: FirebaseStorage

  @Inject lateinit var firestore: FirebaseFirestore

  @Inject lateinit var okHttpClient: OkHttpClient

  @Inject lateinit var settings: Settings

  private var repoDocId: String = ""
  private var pointerFileName: String = ""
  private var pointerType: Int = Constants.POINTER_TOUCH
  private var selectedPointer: Pointer = Pointer()
  private var downloadApkFileName: String = ""
  private var downloadZipFileName: String = ""
  private var magiskModuleSaveName: String = ""

  private val uiState = MutableStateFlow(MagiskRROUiState())

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View = ComposeView(requireContext()).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
      Theme {
        val state by uiState.collectAsStateWithLifecycle()
        MagiskRROScreen(
          state = state,
          onOpenMagiskClick = {
            val (intent, _) = getLaunchIntentForRootManager(requireContext())
            if (intent != null) {
              startActivity(intent)
            } else {
              Toast.makeText(
                requireContext(),
                "No supported Root Manager (Magisk, KernelSU, APatch) found",
                Toast.LENGTH_SHORT,
              ).show()
            }
          },
          onInstallModuleClick = {
            state.modulePath?.let { path ->
              handleInstallModule(path)
            }
          },
        )
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    repoDocId = arguments?.getString("repoDocId") ?: ""
    pointerFileName = arguments?.getString("pointerFileName") ?: ""
    pointerType =
      arguments?.getInt("pointerType", Constants.POINTER_TOUCH) ?: Constants.POINTER_TOUCH

    val isMouse = pointerType == Constants.POINTER_MOUSE
    val stem = pointerFileName.substringBeforeLast(".")
    downloadApkFileName = if (isMouse) "RRO_mouse_$stem.apk" else "RRO_$stem.apk"
    downloadZipFileName = if (isMouse) "RRO_mouse_$stem.zip" else "RRO_$stem.zip"

    val rootManager = getInstalledRootManager(requireContext())
    uiState.update {
      it.copy(
        pointerType = pointerType,
        rootManagerName = if (rootManager != RootManager.UNKNOWN) rootManager.displayName else "Root Manager",
      )
    }

    lifecycleScope.launch {
      setLoading(true)
      val source = if (BuildConfig.DEBUG) Source.DEFAULT else Source.CACHE
      selectedPointer =
        firestore.pointers().document(
          repoDocId,
        ).get(source).await().toObject(Pointer::class.java)
          ?: firestore.pointers().document(
            repoDocId,
          ).get().await().toObject(Pointer::class.java)
          ?: Pointer()

      if (pointerFileName.isBlank() && !selectedPointer.filename.isNullOrBlank()) {
        pointerFileName = selectedPointer.filename!!
      }

      val suffix = if (isMouse) "Mouse_Magisk.zip" else "Magisk.zip"
      magiskModuleSaveName = "${selectedPointer.name}_RRO-2_$suffix"

      val pointerUrl = runCatching {
        storage.pointers().child(pointerFileName).downloadUrl.await().toString()
      }.getOrNull()

      uiState.update {
        it.copy(
          pointerName = selectedPointer.name ?: "Pointer",
          pointerUrl = pointerUrl,
        )
      }

      init()
    }
  }

  private fun init() {
    val selectedPointerModule =
      File(repackedMagiskModulePath(requireContext(), magiskModuleSaveName))
    if (selectedPointerModule.exists()) {
      setModuleReady(selectedPointerModule.path)
      appendLog("- Magisk module already exists at: ${selectedPointerModule.path}")
      MaterialAlertDialogBuilder(requireContext())
        .setTitle("Magisk module exists")
        .setMessage(
          """- Magisk module already exists at: ${selectedPointerModule.path}""".trimMargin(),
        )
        .setPositiveButton("OK") { _, _ -> }
        .show()
      setLoading(false)
      return
    }
    prepareMagiskModule()
  }

  private fun prepareMagiskModule() {
    lifecycleScope.launch {
      setLoading(true)
      // 1. Try downloading pre-packaged flashable Magisk Module ZIP
      val zipDownloaded = downloadRROMagiskZip()
      if (zipDownloaded) {
        val module = File(repackedMagiskModulePath(requireContext(), magiskModuleSaveName))
        if (module.exists()) {
          appendLog("- Magisk module ready at: ${module.path}")
          setModuleReady(module.path)
        }
        setLoading(false)
        return@launch
      }

      // 2. Fallback: Download raw APK and package on device
      appendLog("- Module ZIP not available, downloading APK...")
      val downloaded = downloadRROApk()
      if (!downloaded) {
        appendLog("- Error: Could not download RRO")
        setLoading(false)
        return@launch
      }

      val rroApk = File(rroApkDownloadPath(requireContext()), downloadApkFileName)
      appendLog("- Packaging Magisk Module...")

      val module = withContext(Dispatchers.IO) {
        buildRroMagiskModule(
          context = requireContext(),
          pointerName = selectedPointer.name ?: "Custom",
          rroApkFile = rroApk,
          outputPath = repackedMagiskModulePath(requireContext(), magiskModuleSaveName),
          pointerType = pointerType,
        )
      }

      if (module.exists()) {
        appendLog("- Magisk module saved at: ${module.path}")
        setModuleReady(module.path)
      }
      setLoading(false)
    }
  }

  private fun handleInstallModule(path: String) {
    showRROExperimentalWarning(requireContext()) { response ->
      if (!response) return@showRROExperimentalWarning
      setLoading(true)
      appendLog("- Starting Magisk module installation...")
      installModule(
        path = path,
        callback = { result ->
          Timber.d("MagiskInstall: $result")
          result.err.forEach { error ->
            appendLog("Error: $error")
          }
          if (result.isSuccess) {
            setLoading(false)
            appendLog("- Module installation succeeded")
            settings.applyMethod = Constants.INDEX_RRO_METHOD
            showRebootDialog(requireContext())
          } else {
            appendLog("- Module installation failed")
            setLoading(false)
          }
        },
        onElementAdd = { element ->
          element?.let { appendLog(it) }
        },
      )
    }
  }

  private fun appendLog(text: String) {
    if (text.isBlank()) return
    uiState.update { current ->
      val newLog = if (current.logText.isBlank()) text else "${current.logText}\n$text"
      current.copy(logText = newLog)
    }
  }

  private fun setLoading(loading: Boolean) {
    uiState.update { it.copy(isLoading = loading) }
  }

  private fun setModuleReady(path: String) {
    uiState.update { it.copy(isModuleReady = true, modulePath = path) }
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
      appendLog("- RRO Apk already exists at: ${rroApk.path}")
      return true
    }

    appendLog("- Downloading RRO Apk from: $url")
    withContext(Dispatchers.IO) {
      val request = Request.Builder().url(url).build()
      val response = okHttpClient.newCall(request).execute()
      if (response.isSuccessful) {
        val body = response.body.byteStream()
        val inputStream = body.buffered()
        val outputStream = rroApk.outputStream()
        inputStream.copyTo(outputStream)
        withContext(Dispatchers.Main) {
          appendLog("- RRO Apk saved at: ${rroApk.path}")
          result = true
        }

        if (repoDocId.isNotBlank()) {
          firestore.pointers().document(repoDocId)
            .update(DatabaseFields.FIELD_RRO_DOWNLOADS, FieldValue.increment(1))
        }
      } else {
        withContext(Dispatchers.Main) {
          appendLog("- RRO Apk download failed")
          result = false
        }
      }
    }
    return result
  }

  private suspend fun downloadRROMagiskZip(): Boolean {
    var result = false
    val url =
      "https://github.com/afterroot/allusive-repo/raw/main/modules/$downloadZipFileName"

    val targetFile = File(repackedMagiskModulePath(requireContext(), magiskModuleSaveName))

    if (targetFile.exists()) {
      appendLog("- Magisk module already exists at: ${targetFile.path}")
      return true
    }

    targetFile.parentFile?.mkdirs()

    appendLog("- Downloading Magisk Module ZIP from: $url")
    withContext(Dispatchers.IO) {
      runCatching {
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
          response.body.byteStream().buffered().use { input ->
            targetFile.outputStream().use { output ->
              input.copyTo(output)
            }
          }
          withContext(Dispatchers.Main) {
            appendLog("- Magisk module saved at: ${targetFile.path}")
            result = true
          }

          if (repoDocId.isNotBlank()) {
            firestore.pointers().document(repoDocId)
              .update(DatabaseFields.FIELD_RRO_DOWNLOADS, FieldValue.increment(1))
          }
        }
      }.onFailure { e ->
        Timber.e(e, "downloadRROMagiskZip failed")
      }
    }
    return result
  }
}

@Composable
fun MagiskRROScreen(
  state: MagiskRROUiState,
  onOpenMagiskClick: () -> Unit,
  onInstallModuleClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    if (state.isLoading) {
      LinearProgressIndicator(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(4.dp)),
      )
    }

    // Pointer Header Card
    Card(
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center,
        ) {
          val imageBitmap = ImageBitmap.imageResource(id = CommonR.drawable.transparent_rect)
          val tiledBrush = remember(imageBitmap) {
            ShaderBrush(
              ImageShader(
                image = imageBitmap,
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Repeated,
              ),
            )
          }
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(tiledBrush),
          )

          if (!state.pointerUrl.isNullOrEmpty()) {
            Image(
              painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                  .data(state.pointerUrl)
                  .crossfade(true)
                  .build(),
              ),
              contentDescription = state.pointerName,
              modifier = Modifier.size(56.dp),
            )
          }
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = state.pointerName.ifBlank { stringResource(CommonR.string.text_cur_pointer) },
            style = MaterialTheme.typography.titleMedium,
          )
          val typeStringRes = if (state.pointerType == Constants.POINTER_MOUSE) {
            CommonR.string.pointer_type_mouse
          } else {
            CommonR.string.pointer_type_touch
          }
          Text(
            text = stringResource(typeStringRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // Console Log Box
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
      ),
    ) {
      val scrollState = rememberScrollState()
      LaunchedEffect(state.logText) {
        scrollState.animateScrollTo(scrollState.maxValue)
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(12.dp),
      ) {
        Text(
          text = state.logText.ifBlank { "Initializing..." },
          fontFamily = FontFamily.Monospace,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        )
      }
    }

    // Action Buttons
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedButton(
        onClick = onOpenMagiskClick,
        modifier = Modifier.weight(1f),
      ) {
        val buttonText = if (state.rootManagerName == "Magisk") {
          stringResource(CommonR.string.open_magisk_manager)
        } else {
          "Open ${state.rootManagerName}"
        }
        Text(text = buttonText)
      }

      if (state.isModuleReady) {
        Button(
          onClick = onInstallModuleClick,
          enabled = !state.isLoading,
          modifier = Modifier.weight(1f),
        ) {
          Text(text = stringResource(CommonR.string.install_module))
        }
      }
    }
  }
}

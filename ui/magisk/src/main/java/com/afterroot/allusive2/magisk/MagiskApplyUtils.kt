/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.magisk

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.afterroot.allusive2.Constants
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import timber.log.Timber
import com.afterroot.allusive2.resources.R as CommonR

fun repackedMagiskModulePath(context: Context, name: String) =
  "${context.getExternalFilesDir(null)?.path}/$name"

fun rroApkDownloadPath(context: Context) = "${context.externalCacheDir?.path}/rros"

const val MAGISK_PACKAGE = "com.topjohnwu.magisk"
const val MAGISK_RRO_ZIP = "rro-module-2.zip"

fun copyAssetFile(context: Context, fileName: String, to: String) {
  try {
    context.assets.open(fileName).use { inputStream ->
      val outFile = File(to)
      outFile.parentFile?.mkdirs()
      FileOutputStream(outFile).use { outputStream ->
        inputStream.copyTo(outputStream)
      }
    }
  } catch (e: IOException) {
    Timber.tag("COPY_ASSET").e(e, "Failed to copy asset file: %s", fileName)
  }
}

/**
 * Builds an RRO Magisk module zip directly from the base asset template using Zip4j.
 * Eliminates disk extraction and repacking steps.
 */
fun buildRroMagiskModule(
  context: Context,
  pointerName: String,
  rroApkFile: File,
  outputPath: String,
  pointerType: Int = Constants.POINTER_TOUCH,
): File {
  val outputFile = File(outputPath)
  outputFile.parentFile?.mkdirs()
  if (outputFile.exists()) outputFile.delete()

  // Copy base template directly to output path
  copyAssetFile(context, MAGISK_RRO_ZIP, outputFile.path)

  val isMouse = pointerType == Constants.POINTER_MOUSE
  val targetApkName = if (isMouse) "allusive_rro_mouse.apk" else "allusive_rro.apk"
  val moduleId = if (isMouse) "pointer_replacer_rro_mouse" else "pointer_replacer_rro"
  val displayName = if (isMouse) "Pointer Replacer RRO (Mouse) - $pointerName" else "Pointer Replacer RRO - $pointerName"
  val description = if (isMouse) {
    "Magisk Mouse RRO Overlay for '$pointerName' pointer"
  } else {
    "Magisk RRO Overlay for '$pointerName' pointer"
  }

  // Inject RRO APK and custom module.prop via Zip4j
  val zipFile = ZipFile(outputFile)
  val apkParamsVendor = ZipParameters().apply {
    fileNameInZip = "system/vendor/overlay/$targetApkName"
  }
  zipFile.addFile(rroApkFile, apkParamsVendor)

  val apkParamsProduct = ZipParameters().apply {
    fileNameInZip = "system/product/overlay/$targetApkName"
  }
  zipFile.addFile(rroApkFile, apkParamsProduct)

  val moduleProp = """
    id=$moduleId
    name=$displayName
    version=v2.0
    versionCode=2
    author=thesandipv
    description=$description
  """.trimIndent()

  val propParams = ZipParameters().apply {
    fileNameInZip = "module.prop"
  }
  ByteArrayInputStream(moduleProp.toByteArray(Charsets.UTF_8)).use { stream ->
    zipFile.addStream(stream, propParams)
  }

  return outputFile
}

enum class RootManager(val displayName: String, val packageNames: List<String>) {
  MAGISK("Magisk", listOf("com.topjohnwu.magisk")),
  KERNEL_SU("KernelSU", listOf("io.github.tiann.kernelsu", "io.github.tiann.kernelsu.debug")),
  KSU_NEXT("KernelSU Next", listOf("com.rifsxd.ksunext")),
  APATCH("APatch", listOf("me.bmax.apatch")),
  UNKNOWN("Root Manager", emptyList()),
}

fun getInstalledRootManager(context: Context): RootManager {
  val pm = context.packageManager
  for (manager in RootManager.entries) {
    for (pkg in manager.packageNames) {
      try {
        pm.getPackageInfo(pkg, 0)
        return manager
      } catch (_: PackageManager.NameNotFoundException) {
      }
    }
  }
  return RootManager.UNKNOWN
}

fun getLaunchIntentForRootManager(context: Context): Pair<Intent?, String> {
  val pm = context.packageManager
  for (manager in RootManager.entries) {
    for (pkg in manager.packageNames) {
      val intent = pm.getLaunchIntentForPackage(pkg)
      if (intent != null) {
        return Pair(intent, manager.displayName)
      }
    }
  }
  return Pair(null, RootManager.UNKNOWN.displayName)
}

fun showRebootDialog(context: Context) {
  MaterialAlertDialogBuilder(context)
    .setTitle(CommonR.string.reboot)
    .setMessage("Pointer Applied.")
    .setPositiveButton(CommonR.string.reboot) { _, _ ->
      try {
        reboot()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
    .setNegativeButton(android.R.string.cancel, null)
    .show()
}

fun installModule(path: String, callback: Shell.ResultCallback, onElementAdd: (String?) -> Unit) {
  val callbackList: CallbackList<String> = object : CallbackList<String>() {
    override fun onAddElement(e: String?) {
      onElementAdd(e)
    }
  }
  val command = """
    if command -v magisk >/dev/null 2>&1; then
      magisk --install-module "$path"
    elif command -v ksu >/dev/null 2>&1; then
      ksu module install "$path"
    elif command -v apd >/dev/null 2>&1; then
      apd module install "$path"
    else
      magisk --install-module "$path"
    fi
  """.trimIndent()
  Shell.cmd(command).to(callbackList).submit(callback)
}

fun showRROExperimentalWarning(context: Context, onResponse: (response: Boolean) -> Unit) {
  MaterialAlertDialogBuilder(context)
    .setTitle("Declaration")
    .setMessage(
      "Applying Pointer by Creating RRO Layer is completely experimental. " +
        "It is not guaranteed that it will work for you. By clicking Install, you understand that your device may get stuck in a bootloop. " +
        "Also ensure you are aware of methods to disable root modules in safe mode.",
    )
    .setPositiveButton("Install") { _, _ ->
      onResponse(true)
    }
    .setNegativeButton(android.R.string.cancel) { _, _ ->
      onResponse(false)
    }
    .show()
}

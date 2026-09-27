/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.magisk

import android.content.Context
import android.graphics.Bitmap
import com.afollestad.materialdialogs.MaterialDialog
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

const val FRAMEWORK_APK = "/system/framework/framework-res.apk"
fun frameworkCopyApkPath(context: Context) = "${context.externalCacheDir?.path}/framework.apk"
fun frameworkExtractPath(context: Context) = "${context.externalCacheDir?.path}/framework"
fun pointerSavePath(context: Context) = "${context.externalCacheDir?.path}/pointers"
fun repackedFrameworkPath(context: Context) = "${context.externalCacheDir?.path}/repacked.apk"
fun repackedMagiskModulePath(context: Context, name: String) =
  "${context.getExternalFilesDir(null)?.path}/$name"
fun magiskEmptyModuleZipPath(context: Context) =
  "${context.externalCacheDir?.path}/empty-module.zip"
fun magiskEmptyModuleExtractPath(context: Context) =
  "${context.externalCacheDir?.path}/empty-module"
fun rroApkDownloadPath(context: Context) = "${context.externalCacheDir?.path}/rros"
const val POINTER_XXXHDPI = "/res/drawable-xxxhdpi-v4/pointer_spot_touch.png"
const val POINTER_XXHDPI = "/res/drawable-xxhdpi-v4/pointer_spot_touch.png"
const val POINTER_XHDPI = "/res/drawable-xhdpi-v4/pointer_spot_touch.png"
const val POINTER_HDPI = "/res/drawable-hdpi-v4/pointer_spot_touch.png"
const val POINTER_MDPI = "/res/drawable-mdpi-v4/pointer_spot_touch.png"
const val MAGISK_EMPTY_ZIP = "empty-module.zip"
const val MAGISK_PACKAGE = "com.topjohnwu.magisk"

const val MAGISK_RRO_ZIP = "rro-module-2.zip"
fun magiskRROModuleZipPath(context: Context) = "${context.externalCacheDir?.path}/rro-module-2.zip"
fun magiskRROModuleExtractPath(context: Context) = "${context.externalCacheDir?.path}/rro-module-2"
fun magiskRROSourceApkPath(context: Context) =
  "${magiskRROModuleExtractPath(context)}/system/vendor/overlay/allusive_rro.apk"

fun copyFrameworkRes(context: Context): File {
  val file = File(FRAMEWORK_APK)
  val target = File(frameworkCopyApkPath(context))
  if (target.exists()) target.delete()
  return file.copyTo(target)
}

enum class Variant {
  MDPI,
  HDPI,
  XHDPI,
  XXHDPI,
  XXXHDPI,
}

object VariantSizes {
  const val MDPI = 24
  const val HDPI = 36
  const val XHDPI = 48
  const val XXHDPI = 72
  const val XXXHDPI = 96
}

val ALL_VARIANTS = listOf(
  Variant.MDPI,
  Variant.HDPI,
  Variant.XHDPI,
  Variant.XXHDPI,
  Variant.XXXHDPI,
)

fun Bitmap.saveAs(path: String): File {
  val file = File(path)
  file.parentFile?.mkdirs()
  if (file.exists()) file.delete()
  kotlin.runCatching {
    val fos = FileOutputStream(file)
    this.compress(Bitmap.CompressFormat.PNG, 100, fos)
    fos.flush()
    fos.close()
  }.onFailure {
    Timber.e("saveAs: ${it.cause}")
    it.printStackTrace()
  }
  return file
}

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
): File {
  val outputFile = File(outputPath)
  outputFile.parentFile?.mkdirs()
  if (outputFile.exists()) outputFile.delete()

  // Copy base template directly to output path
  copyAssetFile(context, MAGISK_RRO_ZIP, outputFile.path)

  // Inject RRO APK and custom module.prop via Zip4j
  val zipFile = ZipFile(outputFile)
  val apkParams = ZipParameters().apply {
    fileNameInZip = "system/vendor/overlay/allusive_rro.apk"
  }
  zipFile.addFile(rroApkFile, apkParams)

  val moduleProp = """
    id=pointer_replacer_rro
    name=Pointer Replacer RRO - $pointerName
    version=v2.0
    versionCode=2
    author=thesandipv
    description=Magisk RRO Overlay for '$pointerName' pointer
  """.trimIndent()

  val propParams = ZipParameters().apply {
    fileNameInZip = "module.prop"
  }
  ByteArrayInputStream(moduleProp.toByteArray(Charsets.UTF_8)).use { stream ->
    zipFile.addStream(stream, propParams)
  }

  return outputFile
}

/**
 * Builds a framework-res Magisk module zip directly from the base asset template using Zip4j.
 */
fun buildFrameworkMagiskModule(
  context: Context,
  pointerName: String,
  repackedFrameworkApk: File,
  outputPath: String,
): File {
  val outputFile = File(outputPath)
  outputFile.parentFile?.mkdirs()
  if (outputFile.exists()) outputFile.delete()

  // Copy base template directly to output path
  copyAssetFile(context, MAGISK_EMPTY_ZIP, outputFile.path)

  val zipFile = ZipFile(outputFile)
  val fwParams = ZipParameters().apply {
    fileNameInZip = "system/framework/framework-res.apk"
  }
  zipFile.addFile(repackedFrameworkApk, fwParams)

  val moduleProp = """
    id=pointer_replacer
    name=Pointer Replacer - $pointerName
    version=v2.0
    versionCode=2
    author=thesandipv
    description=Framework-res pointer replacement for '$pointerName'
  """.trimIndent()

  val propParams = ZipParameters().apply {
    fileNameInZip = "module.prop"
  }
  ByteArrayInputStream(moduleProp.toByteArray(Charsets.UTF_8)).use { stream ->
    zipFile.addStream(stream, propParams)
  }

  return outputFile
}

fun showRebootDialog(context: Context) {
  MaterialDialog(context).show {
    title(res = CommonR.string.reboot)
    message(text = "Pointer Applied.")
    positiveButton(res = CommonR.string.reboot) {
      try {
        reboot()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
    negativeButton(android.R.string.cancel) {
    }
  }
}

fun installModule(path: String, callback: Shell.ResultCallback, onElementAdd: (String?) -> Unit) {
  val callbackList: CallbackList<String> = object : CallbackList<String>() {
    override fun onAddElement(e: String?) {
      onElementAdd(e)
    }
  }
  Shell.cmd("magisk --install-module \"${path}\"").to(callbackList).submit(callback)
}

fun showRROExperimentalWarning(context: Context, onResponse: (response: Boolean) -> Unit) {
  MaterialDialog(context).show {
    title(text = "Declaration")
    message(
      text = "Applying Pointer by Creating RRO Layer is completely experimental. " +
        "It's is not guaranteed that it'll work for you. By clicking Install, you understand that your device may stuck in bootloop. " +
        "Also you are aware about methods of disabling magisk.",
    )
    positiveButton(text = "Install") {
      onResponse(true)
    }
    negativeButton(android.R.string.cancel) {
      onResponse(false)
    }
  }
}

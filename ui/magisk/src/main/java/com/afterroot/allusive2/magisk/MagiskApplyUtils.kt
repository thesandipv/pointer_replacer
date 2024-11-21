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

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import com.afollestad.materialdialogs.MaterialDialog
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties
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
const val POINTER_XHDPI = "/res/drawable-xhdpi-v4/pointer_spot_touch.png"
const val POINTER_MDPI = "/res/drawable-mdpi-v4/pointer_spot_touch.png"
const val POINTER_HDPI = "/res/drawable-hdpi-v4/pointer_spot_touch.png"
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

fun filesToReplace(context: Context): List<File> {
  val targetPath = frameworkExtractPath(context)
  if (!File(targetPath).exists()) return emptyList()
  val list = mutableListOf<File>()
  val paths = listOf(
    "$targetPath$POINTER_HDPI",
    "$targetPath$POINTER_MDPI",
    "$targetPath$POINTER_XHDPI",
  )
  paths.forEach {
    val file = File(it)
    if (file.exists()) {
      list.add(file)
    }
  }
  return list
}

enum class Variant {
  MDPI,
  HDPI,
  XHDPI,
}

object VariantSizes {
  const val MDPI = 33
  const val HDPI = 49
  const val XHDPI = 66
}

val ALL_VARIANTS = listOf(Variant.MDPI, Variant.HDPI, Variant.XHDPI)

fun variantsToReplace(context: Context): List<Variant> {
  val targetPath = frameworkExtractPath(context)
  if (!File(targetPath).exists()) return emptyList()

  val list = mutableListOf<Variant>()
  if (File("$targetPath$POINTER_HDPI").exists()) list.add(Variant.HDPI)
  if (File("$targetPath$POINTER_MDPI").exists()) list.add(Variant.MDPI)
  if (File("$targetPath$POINTER_XHDPI").exists()) list.add(Variant.XHDPI)
  return list
}

fun variantsToReplace(targetPath: String): List<Variant> {
  if (!File(targetPath).exists()) return emptyList()

  val list = mutableListOf<Variant>()
  if (File("$targetPath$POINTER_HDPI").exists()) list.add(Variant.HDPI)
  if (File("$targetPath$POINTER_MDPI").exists()) list.add(Variant.MDPI)
  if (File("$targetPath$POINTER_XHDPI").exists()) list.add(Variant.XHDPI)
  return list
}

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

fun copyMagiskEmptyZip(context: Context, to: String) {
  copyAssetFile(context, MAGISK_EMPTY_ZIP, to)
}

fun copyMagiskRROZip(context: Context, to: String) {
  copyAssetFile(context, MAGISK_RRO_ZIP, to)
}

fun extractMagiskZip(context: Context) {
  val file = File(magiskEmptyModuleZipPath(context))
  if (!file.exists()) return
  file.unzip(toFolder = File(magiskEmptyModuleExtractPath(context)))
}

fun copyAssetFile(context: Context, fileName: String, to: String) {
  val assetManager: AssetManager = context.assets
  val inputStream: InputStream?
  val outputStream: OutputStream?
  try {
    inputStream = assetManager.open(fileName)
    val outFile = File(to)
    outputStream = FileOutputStream(outFile)
    inputStream.copyTo(outputStream)
    inputStream.close()
    outputStream.flush()
    outputStream.close()
  } catch (e: IOException) {
    Timber.tag("COPY_ASSET").e(e, "Failed to copy asset file: %s", fileName)
  }
}

fun copyRepackedFrameworkResApk(context: Context): File {
  val repacked = File(repackedFrameworkPath(context))
  return repacked.copyTo(
    target = File("${magiskEmptyModuleExtractPath(context)}$FRAMEWORK_APK"),
    overwrite = true,
  )
}

fun copyDownloadedRROApk(context: Context, dlRROApkFileName: String): File {
  val downloaded = File(rroApkDownloadPath(context), dlRROApkFileName)
  return downloaded.copyTo(target = File(magiskRROSourceApkPath(context)), overwrite = true)
}

fun createModuleProp(context: Context) {
  val properties = Properties()
  properties.load(FileReader(File(magiskEmptyModuleExtractPath(context) + "/module.prop")))
  properties.keys
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
  Shell.su("magisk --install-module \"${path}\"").to(callbackList).submit(callback)
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

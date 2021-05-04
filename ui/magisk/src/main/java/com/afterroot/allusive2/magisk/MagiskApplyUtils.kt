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
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

const val FRAMEWORK_APK = "/system/framework/framework-res.apk"
fun frameworkCopyApkPath(context: Context) = "${context.externalCacheDir?.path}/framework.apk"
fun frameworkExtractPath(context: Context) = "${context.externalCacheDir?.path}/framework"
const val POINTER_XHDPI = "/res/drawable-xhdpi-v4/pointer_spot_touch.png"
const val POINTER_MDPI = "/res/drawable-mdpi-v4/pointer_spot_touch.png"
const val POINTER_HDPI = "/res/drawable-hdpi-v4/pointer_spot_touch.png"

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
        "$targetPath$POINTER_XHDPI"
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
    MDPI, HDPI, XHDPI
}

object VariantSizes {
    const val MDPI = 33
    const val HDPI = 49
    const val XHDPI = 66
}

fun variantsToReplace(context: Context): List<Variant> {
    val targetPath = frameworkExtractPath(context)
    if (!File(targetPath).exists()) return emptyList()

    val list = mutableListOf<Variant>()
    if (File("$targetPath$POINTER_HDPI").exists()) list.add(Variant.HDPI)
    if (File("$targetPath$POINTER_MDPI").exists()) list.add(Variant.MDPI)
    if (File("$targetPath$POINTER_XHDPI").exists()) list.add(Variant.XHDPI)
    return list
}

fun Bitmap.saveAs(path: String): File {
    val file = File(path)
    if (file.exists()) file.delete()
    kotlin.runCatching {
        val fos = FileOutputStream(file)
        this.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.flush()
        fos.close()
    }
    return file
}
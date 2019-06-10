/*
 * Copyright (C) 2016-2019 Sandip Vaghela
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

package com.afterroot.allusive.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.MimeTypeMap

/**
 * Helper Class
 */
object Helper {

    fun isAppInstalled(context: Context, pName: String): Boolean {
        return try {
            context.packageManager.getApplicationInfo(pName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getDpi(context: Context): Int {
        return context.resources.displayMetrics.densityDpi
    }

    /**
     * @fileName fileName name of file
     * @return extension of fileName
     */
    private fun getFileExt(fileName: String): String {
        return fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length)
    }

    /**
     * @fileName fileName name of file
     * @return mime type of fileName
     */
    private fun getMimeType(fileName: String): String? {
        var type: String? = null
        try {
            val extension = getFileExt(fileName)
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return type
    }

    fun openFile(context: Context, filename: String, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, getMimeType(filename))
        context.startActivity(intent)
    }
}

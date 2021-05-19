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

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Throws(IOException::class)
fun File.unzip(toFolder: File, path: String = "", junkPath: Boolean = false) {
    inputStream().buffered().use {
        it.unzip(toFolder, path, junkPath)
    }
}

@Throws(IOException::class)
fun InputStream.unzip(folder: File, path: String, junkPath: Boolean) {
    try {
        val zin = ZipInputStream(this)
        var entry: ZipEntry
        while (true) {
            entry = zin.nextEntry ?: break
            if (!entry.name.startsWith(path) || entry.isDirectory) {
                // Ignore directories, only create files
                continue
            }
            val name = if (junkPath)
                entry.name.substring(entry.name.lastIndexOf('/') + 1)
            else
                entry.name

            val dest = File(folder, name)
            dest.parentFile!!.mkdirs()
            FileOutputStream(dest).use { zin.copyTo(it) }
        }
    } catch (e: IOException) {
        e.printStackTrace()
        throw e
    }
}

var filesListInDir: MutableList<String> = ArrayList()

fun zip(sourceFolder: File, exportPath: String): File {
    filesListInDir.clear()
    runCatching {
        populateFilesList(sourceFolder)
        val fos = FileOutputStream(exportPath)
        val zos = ZipOutputStream(fos)
        for (filePath in filesListInDir) {
            val ze = ZipEntry(filePath.substring(sourceFolder.absolutePath.length + 1, filePath.length))
            zos.putNextEntry(ze)
            val fis = FileInputStream(filePath)
            fis.copyTo(zos)
            zos.closeEntry()
            fis.close()
        }
        zos.close()
        fos.close()
    }.onFailure {
        it.printStackTrace()
    }
    return File(exportPath)
}

@Throws(IOException::class)
private fun populateFilesList(dir: File) {
    val files = dir.listFiles()
    files?.forEach { file ->
        if (file.isFile) filesListInDir.add(file.absolutePath) else populateFilesList(file)
    }
}

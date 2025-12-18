/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.magisk

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.CRC32
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
      val name = if (junkPath) {
        entry.name.substring(entry.name.lastIndexOf("/") + 1)
      } else {
        entry.name
      }

      val dest = File(folder, name)
      dest.parentFile!!.mkdirs()
      if (!dest.canonicalPath.startsWith(folder.path)) {
        throw Exception("Zip extract path not matched")
      }
      FileOutputStream(dest).use { zin.copyTo(it) }
    }
  } catch (e: IOException) {
    e.printStackTrace()
    throw e
  } catch (e: Exception) {
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
      val ze =
        ZipEntry(filePath.substring(sourceFolder.absolutePath.length + 1, filePath.length))
      if (filePath.endsWith(".png") || filePath.endsWith("resources.arsc")) {
        ze.apply {
          method = ZipEntry.STORED
          crc = crc32(filePath)
          size = File(filePath).length()
        }
      }
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

// calculate CRC32 of file
fun crc32(filePath: String): Long {
  val crc = CRC32()
  val fis = FileInputStream(filePath)
  val buffer = ByteArray(1024)
  var read: Int
  while (true) {
    read = fis.read(buffer)
    if (read == -1) break
    crc.update(buffer, 0, read)
  }
  fis.close()
  return crc.value
}

@Throws(IOException::class)
private fun populateFilesList(dir: File) {
  val files = dir.listFiles()
  files?.forEach { file ->
    if (file.isFile) filesListInDir.add(file.absolutePath) else populateFilesList(file)
  }
}

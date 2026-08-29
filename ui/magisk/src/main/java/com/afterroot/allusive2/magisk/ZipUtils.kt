/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.magisk

import java.io.File
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

fun zip(sourceFolder: File, exportPath: String): File {
  val exportFile = File(exportPath)
  exportFile.parentFile?.mkdirs()

  runCatching {
    val files = mutableListOf<File>()
    populateFilesList(sourceFolder, files)

    ZipOutputStream(FileOutputStream(exportFile).buffered()).use { zos ->
      for (file in files) {
        val entryName = file.relativeTo(sourceFolder).path.replace(File.separatorChar, '/')
        val ze = ZipEntry(entryName)
        if (file.name.endsWith(".png") || file.name.endsWith("resources.arsc")) {
          ze.apply {
            method = ZipEntry.STORED
            crc = crc32(file)
            size = file.length()
          }
        }
        zos.putNextEntry(ze)
        file.inputStream().buffered().use { fis ->
          fis.copyTo(zos)
        }
        zos.closeEntry()
      }
    }
  }.onFailure {
    it.printStackTrace()
  }
  return exportFile
}

// calculate CRC32 of file
fun crc32(file: File): Long {
  val crc = CRC32()
  file.inputStream().buffered().use { fis ->
    val buffer = ByteArray(8192)
    var read: Int
    while (true) {
      read = fis.read(buffer)
      if (read == -1) break
      crc.update(buffer, 0, read)
    }
  }
  return crc.value
}

fun crc32(filePath: String): Long = crc32(File(filePath))

private fun populateFilesList(dir: File, result: MutableList<File>) {
  val files = dir.listFiles() ?: return
  for (file in files) {
    if (file.isFile) {
      result.add(file)
    } else if (file.isDirectory) {
      populateFilesList(file, result)
    }
  }
}

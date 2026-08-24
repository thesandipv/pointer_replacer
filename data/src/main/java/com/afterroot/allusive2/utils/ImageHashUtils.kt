/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.utils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.scale
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object ImageHashUtils {

  /**
   * Calculates SHA-256 hash for a given file.
   */
  fun calculateSHA256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    FileInputStream(file).use { fis ->
      val buffer = ByteArray(8192)
      var bytesRead: Int
      while (fis.read(buffer).also { bytesRead = it } != -1) {
        digest.update(buffer, 0, bytesRead)
      }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  /**
   * Calculates SHA-256 hash for a given byte array.
   */
  fun calculateSHA256(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(bytes)
    return hash.joinToString("") { "%02x".format(it) }
  }

  /**
   * Calculates Difference Hash (dHash) for perceptual image comparison.
   * Returns a 16-character hex string representing a 64-bit perceptual hash.
   */
  fun calculateDHash(bitmap: Bitmap): String {
    // Resize to 9x8 for 64 comparisons (8 rows of 8 comparisons)
    val scaled = bitmap.scale(9, 8, filter = true)
    var hash = 0L

    for (y in 0 until 8) {
      for (x in 0 until 8) {
        val leftPixel = scaled.getPixel(x, y)
        val rightPixel = scaled.getPixel(x + 1, y)

        val leftLuminance = getLuminance(leftPixel)
        val rightLuminance = getLuminance(rightPixel)

        if (leftLuminance > rightLuminance) {
          val bitPosition = y * 8 + x
          hash = hash or (1L shl bitPosition)
        }
      }
    }

    return "%016x".format(hash)
  }

  /**
   * Calculates Hamming distance between two hex dHash strings.
   * Returns the number of differing bits (0 to 64).
   * A distance of <= 5 typically indicates identical or near-identical images.
   */
  fun hammingDistance(hash1: String, hash2: String): Int {
    if (hash1.length != hash2.length) return 64
    val val1 = hash1.toULongOrNull(16) ?: return 64
    val val2 = hash2.toULongOrNull(16) ?: return 64
    var diff = val1 xor val2
    var distance = 0
    while (diff != 0UL) {
      distance += (diff and 1UL).toInt()
      diff = diff shr 1
    }
    return distance
  }

  private fun getLuminance(color: Int): Double {
    val alpha = Color.alpha(color)
    if (alpha == 0) return 0.0 // Transparent treated as black/empty
    val red = Color.red(color)
    val green = Color.green(color)
    val blue = Color.blue(color)
    return 0.299 * red + 0.587 * green + 0.114 * blue
  }
}

/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.utils

object ModerationUtils {

  private val BLOCKED_WORDS = listOf(
    "nude", "porn", "xxx", "nsfw", "sex", "pussy", "dick", "cock",
    "boobs", "vagina", "penis", "fuck", "bitch", "asshole", "bastard",
  )

  /**
   * Checks if the given text contains obvious explicit or toxic keywords.
   */
  fun containsExplicitContent(vararg texts: String?): Boolean {
    for (text in texts) {
      if (text.isNullOrBlank()) continue
      val lower = text.lowercase()
      for (word in BLOCKED_WORDS) {
        // Regex word boundary match
        val regex = Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE)
        if (regex.containsMatchIn(lower)) {
          return true
        }
      }
    }
    return false
  }

  /**
   * Validates title and description requirements.
   */
  fun isValidPointerText(name: String?, desc: String?): Boolean {
    if (name.isNullOrBlank() || name.trim().length < 2) return false
    if (containsExplicitContent(name, desc)) return false
    return true
  }
}

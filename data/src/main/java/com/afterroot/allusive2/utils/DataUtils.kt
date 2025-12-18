/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.utils

import com.afterroot.data.utils.FirebaseUtils

fun getMailBodyForFeedback(
  firebaseUtils: FirebaseUtils,
  version: String,
  versionCode: Int,
): String {
  val builder = StringBuilder().apply {
    appendLine("----Do not remove this info----")
    appendLine("Version : $version")
    appendLine("Version Code : $versionCode")
    appendLine("User ID : ${firebaseUtils.uid}")
    appendLine("----Do not remove this info----")
  }
  return builder.toString()
}

/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2

import android.content.Context

object Constants {
  const val POINTER_MOUSE = 2
  const val POINTER_TOUCH = 1
  const val PREF_KEY_FIRST_INSTALL = "first_install_2"
  const val RC_LOGIN = 42
  const val RC_PERMISSION = 256
  const val RC_PICK_IMAGE: Int = 478
  const val PREF_KEY_THEME = "key_app_theme"
  const val PLACEHOLDER_1 = "Lorem"
  const val PLACEHOLDER_2 = "Lorem Ispum"
  const val PLACEHOLDER_3 = "Lorem Ispum Dolor"

  const val INDEX_XPOSED_METHOD = 0
  const val INDEX_FW_RES_METHOD = 1
  const val INDEX_RRO_METHOD = 2
}

object Reason {
  // TODO Design Admin UI
  const val OK = 0
  const val DUPLICATE = 1
  const val NOT_A_POINTER = 2
  const val OTHER = 3
  const val NOTICE = 4
}

fun Context.getMinPointerSize(): Int = this.resources.getInteger(
  com.afterroot.allusive2.resources.R.integer.min_pointer_size,
)

fun Context.getMinPointerSizePx(): Int =
  (this.getMinPointerSize() * this.resources.displayMetrics.density.toInt()) / 160

fun Context.getPointerSaveDir(): String = getPointerSaveRootDir() +
  getString(com.afterroot.allusive2.resources.R.string.pointer_folder_path_new)

fun Context.getPointerSaveRootDir(): String = filesDir.path

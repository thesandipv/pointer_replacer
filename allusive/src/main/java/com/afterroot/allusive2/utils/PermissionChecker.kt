/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class PermissionChecker(private val mContext: Context) {

  fun lacksPermissions(permissions: Array<String>): Boolean = permissions.any {
    lacksPermission(it)
  }

  private fun lacksPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(mContext, permission) ==
      PackageManager.PERMISSION_DENIED
}

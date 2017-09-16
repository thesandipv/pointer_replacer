/*
 * Copyright (C) 2016 Sandip Vaghela
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

package com.afterroot.allusive

import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat

internal class PermissionChecker(private val mContext: Context) {

    fun lacksPermissions(vararg permissions: String): Boolean {
        return permissions.any { lacksPermission(it) }
    }

    private fun lacksPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(mContext, permission) == PackageManager.PERMISSION_DENIED
    }
}


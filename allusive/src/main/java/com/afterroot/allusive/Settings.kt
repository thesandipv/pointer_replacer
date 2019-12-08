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

package com.afterroot.allusive

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.afterroot.allusive.database.DatabaseFields
import com.afterroot.core.extensions.getPrefs

class Settings(context: Context) {

    private val preferences: SharedPreferences = context.getPrefs()
    private val mContext = context

    var pointerPath
        get() = preferences.getString(mContext.getString(R.string.key_pointerPath), null)
        set(value) = preferences.edit(true) {
            putString(mContext.getString(R.string.key_pointerPath), value)
        }

    var mousePath
        get() = preferences.getString(mContext.getString(R.string.key_mousePath), null)
        set(value) = preferences.edit(true) {
            putString(mContext.getString(R.string.key_mousePath), value)
        }

    var selectedPointerPath
        get() = preferences.getString(mContext.getString(R.string.key_selectedPointerPath), null)
        set(value) = preferences.edit(true) {
            putString(mContext.getString(R.string.key_selectedPointerPath), value)
        }

    var selectedMousePath
        get() = preferences.getString(mContext.getString(R.string.key_selectedMousePath), null)
        set(value) = preferences.edit(true) {
            putString(mContext.getString(R.string.key_selectedMousePath), value)
        }

    var pointerSize
        get() = preferences.getInt(mContext.getString(R.string.key_pointerSize), mContext.getMinPointerSize())
        set(value) = preferences.edit(true) {
            putInt(mContext.getString(R.string.key_pointerSize), value)
        }

    var mouseSize
        get() = preferences.getInt(mContext.getString(R.string.key_mouseSize), mContext.getMinPointerSize())
        set(value) = preferences.edit(true) {
            putInt(mContext.getString(R.string.key_mouseSize), value)
        }

    var pointerPadding
        get() = preferences.getInt(mContext.getString(R.string.key_pointerPadding), 0)
        set(value) = preferences.edit(true) {
            putInt(mContext.getString(R.string.key_pointerPadding), value)
        }

    var mousePadding
        get() = preferences.getInt(mContext.getString(R.string.key_mousePadding), 0)
        set(value) = preferences.edit(true) {
            putInt(mContext.getString(R.string.key_mousePadding), value)
        }

    var pointerColor
        get() = preferences.getInt(mContext.getString(R.string.key_pointerColor), 0)
        set(value) = preferences.edit(true) {
            putInt(mContext.getString(R.string.key_pointerColor), value)
        }

    var mouseColor
        get() = preferences.getInt(mContext.getString(R.string.key_mouseColor), 0)
        set(value) = preferences.edit(true) {
            putInt(mContext.getString(R.string.key_mouseColor), value)
        }

    var pointerAlpha
        get() = preferences.getInt(mContext.getString(R.string.key_pointerAlpha), 255)
        set(value) = preferences.edit(true) {
            putInt(mContext.getString(R.string.key_pointerAlpha), value)
        }

    var mouseAlpha
        get() = preferences.getInt(mContext.getString(R.string.key_mouseAlpha), 255)
        set(value) = preferences.edit(true) {
            putInt(mContext.getString(R.string.key_mouseAlpha), value)
        }

    var maxPointerSize
        get() = preferences.getInt(mContext.getString(R.string.key_maxPointerSize), 100)
        set(value) = preferences.edit(true) {
            putInt(mContext.getString(R.string.key_maxPointerSize), value)
        }

    var maxPointerPadding
        get() = preferences.getInt(mContext.getString(R.string.key_maxPaddingSize), 100)
        set(value) = preferences.edit(true) {
            putInt(mContext.getString(R.string.key_maxPaddingSize), value)
        }

    var isEnableAlpha
        get() = preferences.getBoolean(mContext.getString(R.string.key_EnablePointerAlpha), false)
        set(value) = preferences.edit(true) {
            putBoolean(mContext.getString(R.string.key_EnablePointerAlpha), value)
        }

    var pointerTmpColor
        get() = preferences.getInt("POINTER_TMP_COLOR", 0)
        set(value) = preferences.edit(true) {
            putInt("POINTER_TMP_COLOR", value)
        }

    var mouseTmpColor
        get() = preferences.getInt("MOUSE_TMP_COLOR", 0)
        set(value) = preferences.edit(true) {
            putInt("MOUSE_TMP_COLOR", value)
        }

    var isExtDialogCancelled
        get() = preferences.getBoolean(mContext.getString(R.string.key_ext_dialog_cancel), false)
        set(value) = preferences.edit(true) {
            putBoolean(mContext.getString(R.string.key_ext_dialog_cancel), value)
        }
    var isShowTouches
        get() = preferences.getBoolean(mContext.getString(R.string.key_show_touches), false)
        set(value) = preferences.edit(true) {
            putBoolean(mContext.getString(R.string.key_show_touches), value)
        }
    var orderBy
        get() = preferences.getString(mContext.getString(R.string.key_repo_order_by), DatabaseFields.FIELD_TIME)
        set(value) = preferences.edit(true) {
            putString(mContext.getString(R.string.key_repo_order_by), value)
        }

}
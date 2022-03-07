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
package com.afterroot.allusive2

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.afterroot.allusive2.data.R
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.core.extensions.getPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class Settings @Inject constructor(@ApplicationContext val context: Context) {

    private val preferences: SharedPreferences = context.getPrefs()
    fun putString(key: String, value: String?) = preferences.edit(true) {
        putString(key, value)
    }

    fun getString(key: String, value: String?) = preferences.getString(key, value)

    fun putInt(key: String, value: Int) = preferences.edit(true) {
        putInt(key, value)
    }

    fun getInt(key: String, value: Int) = preferences.getInt(key, value)

    fun putBoolean(key: String, value: Boolean) = preferences.edit(true) {
        putBoolean(key, value)
    }

    fun getBoolean(key: String, value: Boolean) = preferences.getBoolean(key, value)

    var pointerPath
        get() = preferences.getString(context.getString(R.string.key_pointerPath), null)
        set(value) = putString(context.getString(R.string.key_pointerPath), value)

    var mousePath
        get() = preferences.getString(context.getString(R.string.key_mousePath), null)
        set(value) = putString(context.getString(R.string.key_mousePath), value)

    var selectedPointerPath
        get() = preferences.getString(context.getString(R.string.key_selectedPointerPath), null)
        set(value) = putString(context.getString(R.string.key_selectedPointerPath), value)

    var appliedPointerSize
        get() = preferences.getInt(context.getString(R.string.key_appliedPointerSize), pointerSize)
        set(value) = putInt(context.getString(R.string.key_appliedPointerSize), value)

    var appliedPointerPadding
        get() = preferences.getInt(context.getString(R.string.key_appliedPointerPadding), pointerPadding)
        set(value) = putInt(context.getString(R.string.key_appliedPointerPadding), value)

    var selectedMousePath
        get() = preferences.getString(context.getString(R.string.key_selectedMousePath), null)
        set(value) = putString(context.getString(R.string.key_selectedMousePath), value)

    var appliedMouseSize
        get() = preferences.getInt(context.getString(R.string.key_appliedMouseSize), mouseSize)
        set(value) = putInt(context.getString(R.string.key_appliedMouseSize), value)

    var appliedMousePadding
        get() = preferences.getInt(context.getString(R.string.key_appliedMousePadding), mousePadding)
        set(value) = putInt(context.getString(R.string.key_appliedMousePadding), value)

    var selectedPointerName
        get() = preferences.getString(context.getString(R.string.key_selectedPointerName), null)
        set(value) = putString(context.getString(R.string.key_selectedPointerName), value)

    var selectedMouseName
        get() = preferences.getString(context.getString(R.string.key_selectedMouseName), null)
        set(value) = putString(context.getString(R.string.key_selectedMouseName), value)

    var pointerSize
        get() = preferences.getInt(context.getString(R.string.key_pointerSize), context.getMinPointerSize())
        set(value) = putInt(context.getString(R.string.key_pointerSize), value)

    var mouseSize
        get() = preferences.getInt(context.getString(R.string.key_mouseSize), context.getMinPointerSize())
        set(value) = putInt(context.getString(R.string.key_mouseSize), value)

    var pointerPadding
        get() = preferences.getInt(context.getString(R.string.key_pointerPadding), 0)
        set(value) = putInt(context.getString(R.string.key_pointerPadding), value)

    var mousePadding
        get() = preferences.getInt(context.getString(R.string.key_mousePadding), 0)
        set(value) = putInt(context.getString(R.string.key_mousePadding), value)

    var pointerColor
        get() = preferences.getInt(context.getString(R.string.key_pointerColor), 0)
        set(value) = putInt(context.getString(R.string.key_pointerColor), value)

    var mouseColor
        get() = preferences.getInt(context.getString(R.string.key_mouseColor), 0)
        set(value) = putInt(context.getString(R.string.key_mouseColor), value)

    var pointerAlpha
        get() = preferences.getInt(context.getString(R.string.key_pointerAlpha), 255)
        set(value) = putInt(context.getString(R.string.key_pointerAlpha), value)

    var mouseAlpha
        get() = preferences.getInt(context.getString(R.string.key_mouseAlpha), 255)
        set(value) = putInt(context.getString(R.string.key_mouseAlpha), value)

    var maxPointerSize
        get() = preferences.getInt(context.getString(R.string.key_maxPointerSize), 100)
        set(value) = putInt(context.getString(R.string.key_maxPointerSize), value)

    var maxPointerPadding
        get() = preferences.getInt(context.getString(R.string.key_maxPaddingSize), 100)
        set(value) = putInt(context.getString(R.string.key_maxPaddingSize), value)

    var isEnableAlpha
        get() = preferences.getBoolean(context.getString(R.string.key_EnablePointerAlpha), false)
        set(value) = putBoolean(context.getString(R.string.key_EnablePointerAlpha), value)

    var pointerTmpColor
        get() = preferences.getInt("POINTER_TMP_COLOR", 0)
        set(value) = putInt("POINTER_TMP_COLOR", value)

    var mouseTmpColor
        get() = preferences.getInt("MOUSE_TMP_COLOR", 0)
        set(value) = putInt("MOUSE_TMP_COLOR", value)

    var isExtDialogCancelled
        get() = preferences.getBoolean(context.getString(R.string.key_ext_dialog_cancel), false)
        set(value) = putBoolean(context.getString(R.string.key_ext_dialog_cancel), value)

    var isShowTouches
        get() = preferences.getBoolean(context.getString(R.string.key_show_touches), false)
        set(value) = putBoolean(context.getString(R.string.key_show_touches), value)

    var orderBy
        get() = preferences.getString(context.getString(R.string.key_repo_order_by), DatabaseFields.FIELD_TIME)
            ?: DatabaseFields.FIELD_TIME
        set(value) = putString(context.getString(R.string.key_repo_order_by), value)

    var filterUserPointers
        get() = getBoolean(context.getString(R.string.key_filter_user_pointers), false)
        set(value) = putBoolean(context.getString(R.string.key_filter_user_pointers), value)

    var safUri
        get() = preferences.getString(context.getString(R.string.key_saf_uri), null)
        set(value) = putString(context.getString(R.string.key_saf_uri), value)

    var isFirstInstalled
        get() = preferences.getBoolean(Constants.PREF_KEY_FIRST_INSTALL, true)
        set(value) = putBoolean(Constants.PREF_KEY_FIRST_INSTALL, value)

    val theme: String?
        get() = preferences.getString(Constants.PREF_KEY_THEME, context.getString(R.string.theme_device_default))
}

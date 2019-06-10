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

package com.afterroot.allusive.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.preference.PreferenceManager
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat

fun View.visible(visibility: Boolean) {
    this.visibility = if (visibility) View.VISIBLE else View.GONE
}

fun Context.isAppInstalled(pName: String): Boolean {
    return try {
        this.packageManager.getApplicationInfo(pName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

fun Context.isNetworkAvailable(): Boolean {
    val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = cm.activeNetworkInfo
    return networkInfo != null && networkInfo.isConnected
}

fun Context.getPrefs(): SharedPreferences {
    return PreferenceManager.getDefaultSharedPreferences(this)
}

fun Activity.getDrawableExt(id: Int, tint: Int? = null): Drawable {
    val drawable = ContextCompat.getDrawable(this, id)
    if (tint != null) {
        DrawableCompat.setTint(drawable!!, tint)
    }
    return drawable!!
}

fun Context.getDrawableExt(id: Int, tint: Int? = null): Drawable {
    val drawable = ContextCompat.getDrawable(this, id)
    if (tint != null) {
        DrawableCompat.setTint(drawable!!, tint)
    }
    return drawable!!
}


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

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Uri
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afterroot.allusive.R
import kotlinx.android.synthetic.main.dialog_progress.view.*

/**
 * sets visibility of view with optional transition
 * last updated - 03-09-2019
 */
fun View.visible(
    value: Boolean,
    transition: Transition? = Fade(if (value) Fade.MODE_IN else Fade.MODE_OUT),
    view: ViewGroup = parent as ViewGroup
) {
    if (transition != null) {
        TransitionManager.beginDelayedTransition(view, transition)
    }
    visibility = if (value) View.VISIBLE else View.GONE
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

/**
 * returns drawable with optional tint
 * last updated - 31-08-2019
 */
fun Context.getDrawableExt(@DrawableRes id: Int, @ColorRes tint: Int? = null): Drawable {
    val drawable = ContextCompat.getDrawable(this, id)
    if (tint != null) {
        DrawableCompat.setTint(drawable!!, ContextCompat.getColor(this, tint))
    }
    return drawable!!
}

fun Context.getMinPointerSize(): Int = this.resources.getInteger(R.integer.min_pointer_size)

/**
 * @fileName fileName name of file
 * @return extension of fileName
 */
fun getFileExt(fileName: String): String {
    return fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length)
}

/**
 * @fileName fileName name of file
 * @return mime type of fileName
 */
fun getMimeType(fileName: String): String? {
    var type: String? = null
    try {
        val extension = getFileExt(fileName)
        type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return type
}

fun openFile(context: Context, filename: String, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(uri, getMimeType(filename))
    context.startActivity(intent)
}

/**
 * Extension Function for Inflating Layout to ViewGroup
 */
fun ViewGroup.inflate(layoutId: Int, attachToRoot: Boolean = false): View =
    LayoutInflater.from(context).inflate(layoutId, this, attachToRoot)

fun loadBitmapFromView(view: View): Bitmap? {
    if (view.width == 0 || view.height == 0) {
        return null
    }
    val b = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    view.run {
        layout(view.left, view.top, view.right, view.bottom)
        draw(c)
    }
    return b
}

fun Context.showStaticProgressDialog(progressText: String): MaterialDialog {
    val dialog = MaterialDialog(this).show {
        customView(R.layout.dialog_progress)
        cornerRadius(16f)
        cancelable(false)
    }
    val customView = dialog.getCustomView()
    customView.text_progress.text = progressText
    return dialog
}
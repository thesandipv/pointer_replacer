/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.ui.common.compose.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.resources.R

@Composable
fun Theme(context: Context, content: @Composable () -> Unit) {
  val settings = Settings(context)
  val colorScheme: ColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    when (settings.theme) {
      context.getString(R.string.theme_light) -> dynamicLightColorScheme(context)
      else -> dynamicDarkColorScheme(context)
    }
  } else {
    when (settings.theme) {
      context.getString(R.string.theme_light) -> lightColorScheme()
      else -> darkColorScheme()
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = MaterialTheme.typography,
    content = content,
  )
}

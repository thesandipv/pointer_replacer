/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.ui.common.compose.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.resources.R

@Composable
fun Theme(content: @Composable () -> Unit) {
  val context = LocalContext.current

  val settings = Settings(context)
  val colorScheme: ColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    when (settings.theme) {
      stringResource(R.string.theme_light) -> dynamicLightColorScheme(context)
      else -> dynamicDarkColorScheme(context)
    }
  } else {
    when (settings.theme) {
      stringResource(R.string.theme_light) -> lightColorScheme()
      else -> darkColorScheme()
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = MaterialTheme.typography,
    content = {
      CompositionLocalProvider(
        LocalContentColor provides contentColorFor(backgroundColor = colorScheme.surface),
        content = content,
      )
    },
  )
}

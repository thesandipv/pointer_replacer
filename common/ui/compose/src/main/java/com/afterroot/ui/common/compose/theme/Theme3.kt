/*
 * Copyright (C) 2016-2022 Sandip Vaghela
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

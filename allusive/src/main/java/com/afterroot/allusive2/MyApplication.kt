/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2

import android.app.Application
import androidx.annotation.Keep
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
@Keep
@HiltAndroidApp
class MyApplication : Application() {
  override fun onCreate() {
    DynamicColors.applyToActivitiesIfAvailable(this)
    super.onCreate()
  }
}

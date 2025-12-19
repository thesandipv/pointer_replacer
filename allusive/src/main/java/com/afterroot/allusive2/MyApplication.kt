/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2

import androidx.annotation.Keep
import androidx.multidex.MultiDexApplication
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp

@Keep
@HiltAndroidApp
class MyApplication : MultiDexApplication() {
  override fun onCreate() {
    DynamicColors.applyToActivitiesIfAvailable(this)
    super.onCreate()
  }
}

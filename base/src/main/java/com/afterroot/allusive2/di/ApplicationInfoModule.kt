/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */

package com.afterroot.allusive2.di

import android.app.Application
import app.tivi.app.ApplicationInfo
import com.afterroot.allusive2.base.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ApplicationInfoModule {
  @Provides
  fun provideApplicationInfo(
    @VersionCode
    versionCode: Int,
    @VersionName
    versionName: String,
    application: Application,
  ): ApplicationInfo = ApplicationInfo(
    packageName = application.packageName,
    debugBuild = BuildConfig.DEBUG,
    versionCode = versionCode,
    versionName = versionName,
  )
}

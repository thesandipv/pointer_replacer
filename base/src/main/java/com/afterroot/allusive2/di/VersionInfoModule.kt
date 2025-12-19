/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright (C) 2021-2024 AfterROOT
 */

package com.afterroot.allusive2.di

import com.afterroot.allusive2.base.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class VersionName

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class VersionCode

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class VersionFormatted

@Module
@InstallIn(SingletonComponent::class)
object VersionInfoModule {
  @Provides
  @VersionCode
  fun provideVersionCode(): Int = BuildConfig.VERSION_CODE

  @Provides
  @VersionName
  fun provideVersionName(): String = BuildConfig.VERSION_NAME

  @Provides
  @VersionFormatted
  fun provideVersionString() =
    "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) - ${BuildConfig.COMMIT_ID}"
}

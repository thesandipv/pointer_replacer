/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright (C) 2021-2024 AfterROOT
 */

package com.afterroot.allusive2.di

import app.tivi.app.ApplicationInfo
import app.tivi.util.AndroidSetCrashReportingEnabledAction
import app.tivi.util.CompositeLogger
import app.tivi.util.Logger
import app.tivi.util.NoopRecordingLogger
import app.tivi.util.RecordingLogger
import app.tivi.util.RecordingLoggerImpl
import app.tivi.util.SetCrashReportingEnabledAction
import app.tivi.util.TimberLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoggingModule {
  @Provides
  @Singleton
  fun provideLogger(timberLogger: TimberLogger, recordingLogger: RecordingLogger): Logger =
    CompositeLogger(timberLogger, recordingLogger)

  @Provides
  @Singleton
  fun provideRecordingLogger(applicationInfo: ApplicationInfo): RecordingLogger = when {
    applicationInfo.debugBuild -> RecordingLoggerImpl()
    else -> NoopRecordingLogger
  }

  @Provides
  @Singleton
  fun provideSetCrashReportingEnabledAction(
    androidSetCrashReportingEnabledAction: AndroidSetCrashReportingEnabledAction,
  ): SetCrashReportingEnabledAction = androidSetCrashReportingEnabledAction
}

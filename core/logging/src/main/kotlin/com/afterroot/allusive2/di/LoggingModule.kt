/*
 * Copyright (C) 2016-2024 Sandip Vaghela
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

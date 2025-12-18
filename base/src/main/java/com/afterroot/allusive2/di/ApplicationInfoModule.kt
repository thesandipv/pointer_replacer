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

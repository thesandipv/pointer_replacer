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

package com.afterroot.allusive2.repository

import com.afterroot.allusive2.data.model.DarkThemeConfig
import com.afterroot.allusive2.data.model.UserData
import com.afterroot.allusive2.datastore.UserSettingsDataSource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class UserDataRepositoryImpl @Inject constructor(
    private val userSettingsDataSource: UserSettingsDataSource,
) : UserDataRepository {
    override val userData: Flow<UserData>
        get() = userSettingsDataSource.data

    override suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {
        userSettingsDataSource.setDarkThemeConfig(darkThemeConfig)
    }

    override suspend fun setDynamicColorPreference(useDynamicColor: Boolean) {
        userSettingsDataSource.setUseDynamicColor(useDynamicColor)
    }

    override suspend fun setIsOnboarded(value: Boolean) {
        userSettingsDataSource.setIsOnboarded(value)
    }

    override suspend fun enableFirebaseEmulators(value: Boolean) {
        userSettingsDataSource.setEnableFirebaseEmulators(value)
    }
}

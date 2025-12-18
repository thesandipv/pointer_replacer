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

package com.afterroot.allusive2.datastore

import androidx.datastore.core.DataStore
import app.tivi.util.Logger
import com.afterroot.allusive2.data.model.DarkThemeConfig
import com.afterroot.allusive2.data.model.UserData
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class UserSettingsDataSource @Inject constructor(
  private val userSettings: DataStore<UserSettings>,
  private val logger: Logger,
) {
  val data = userSettings.data.map {
    UserData(
      isOnboarded = it.isOnboarded,
      useDynamicColor = it.useDynamicColor,
      isUserSignedIn = false,
      darkThemeConfig = when (it.darkThemeConfig) {
        null,
        DarkThemeConfigProto.DARK_THEME_CONFIG_UNSPECIFIED,
        DarkThemeConfigProto.UNRECOGNIZED,
        DarkThemeConfigProto.DARK_THEME_CONFIG_FOLLOW_SYSTEM,
        -> DarkThemeConfig.FOLLOW_SYSTEM

        DarkThemeConfigProto.DARK_THEME_CONFIG_LIGHT -> DarkThemeConfig.LIGHT
        DarkThemeConfigProto.DARK_THEME_CONFIG_DARK -> DarkThemeConfig.DARK
      },
      enableFirebaseEmulators = it.enableFirebaseEmulator,
    )
  }

  suspend fun setUseDynamicColor(value: Boolean) = updateData { it.useDynamicColor = value }

  suspend fun setIsOnboarded(value: Boolean) = updateData { it.isOnboarded = value }

  suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) = updateData {
    it.darkThemeConfig = when (darkThemeConfig) {
      DarkThemeConfig.FOLLOW_SYSTEM -> DarkThemeConfigProto.DARK_THEME_CONFIG_FOLLOW_SYSTEM
      DarkThemeConfig.LIGHT -> DarkThemeConfigProto.DARK_THEME_CONFIG_LIGHT
      DarkThemeConfig.DARK -> DarkThemeConfigProto.DARK_THEME_CONFIG_DARK
    }
  }

  suspend fun setEnableFirebaseEmulators(value: Boolean) = updateData {
    it.enableFirebaseEmulator = value
  }

  private suspend fun updateData(block: (UserSettingsKt.Dsl) -> Unit) = try {
    userSettings.updateData {
      it.copy {
        block(this)
      }
    }
  } catch (ioException: IOException) {
    logger.e(ioException) { "Failed to update settings" }
  }
}

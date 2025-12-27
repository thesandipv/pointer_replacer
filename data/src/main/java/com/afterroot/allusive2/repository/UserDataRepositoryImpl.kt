/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
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

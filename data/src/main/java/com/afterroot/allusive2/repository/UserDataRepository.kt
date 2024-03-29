/*
 * Copyright (C) 2021-2024 AfterROOT
 */

package com.afterroot.allusive2.repository

import com.afterroot.allusive2.data.model.DarkThemeConfig
import com.afterroot.allusive2.data.model.UserData
import kotlinx.coroutines.flow.Flow

interface UserDataRepository {
    val userData: Flow<UserData>
    suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig)
    suspend fun setDynamicColorPreference(useDynamicColor: Boolean)
    suspend fun setIsOnboarded(value: Boolean)
    suspend fun enableFirebaseEmulators(value: Boolean)
}

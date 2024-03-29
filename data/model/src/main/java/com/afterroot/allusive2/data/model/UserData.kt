/*
 * Copyright (C) 2021-2024 AfterROOT
 */

package com.afterroot.allusive2.data.model

data class UserData(
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    val useDynamicColor: Boolean = true,
    val isUserSignedIn: Boolean = false,
    val isOnboarded: Boolean = false,
    val enableFirebaseEmulators: Boolean = false,
)

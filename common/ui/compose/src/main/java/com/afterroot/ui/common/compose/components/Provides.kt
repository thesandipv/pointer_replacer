/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.ui.common.compose.components

import androidx.compose.runtime.compositionLocalOf
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.model.LocalUser

val LocalCurrentUser = compositionLocalOf { LocalUser() }

val LocalSettings =
  compositionLocalOf<Settings> { throw IllegalStateException("LocalSettings is not initialized") }

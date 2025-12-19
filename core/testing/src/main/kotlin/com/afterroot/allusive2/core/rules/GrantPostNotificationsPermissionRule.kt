/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */

package com.afterroot.allusive2.core.rules

import android.Manifest.permission.POST_NOTIFICATIONS
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.test.rule.GrantPermissionRule.grant
import org.junit.rules.TestRule

/**
 * [TestRule] granting [POST_NOTIFICATIONS] permission if running on [SDK_INT] greater than [TIRAMISU].
 */
class GrantPostNotificationsPermissionRule :
  TestRule by if (SDK_INT >= TIRAMISU) grant(POST_NOTIFICATIONS) else grant()

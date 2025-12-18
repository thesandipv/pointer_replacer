/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */

package com.afterroot.allusive2.core.testing

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Before
import org.junit.Rule
import org.robolectric.annotation.Config

@HiltAndroidTest
@Config(application = HiltTestApplication::class, manifest = Config.NONE)
abstract class AppTest {
  @get:Rule(order = 0)
  val hiltRule: HiltAndroidRule by lazy { HiltAndroidRule(this) }

  @Before
  fun init() {
    hiltRule.inject()
  }
}

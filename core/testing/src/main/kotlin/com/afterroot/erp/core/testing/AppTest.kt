/*
 * Copyright (C) 2021-2024 AfterROOT
 */
package com.afterroot.erp.core.testing

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

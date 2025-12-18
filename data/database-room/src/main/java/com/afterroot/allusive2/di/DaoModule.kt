/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */

package com.afterroot.allusive2.di

import com.afterroot.allusive2.database.MyDatabase
import com.afterroot.allusive2.database.PointerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
  @Provides
  fun providePointersDao(myDatabase: MyDatabase): PointerDao = myDatabase.pointerDao()
}

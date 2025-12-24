/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */

package com.afterroot.allusive2.di

import com.afterroot.allusive2.repository.UserDataRepository
import com.afterroot.allusive2.repository.UserDataRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
  @Binds
  abstract fun bindsUserDataRepository(
    userDataRepository: UserDataRepositoryImpl,
  ): UserDataRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataModuleProvides {
  @Provides
  @Singleton
  fun provideJson(): Json = Json {
    ignoreUnknownKeys = true
  }
}

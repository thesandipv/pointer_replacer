/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */

package com.afterroot.allusive2.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.afterroot.allusive2.base.CoroutineDispatchers
import com.afterroot.allusive2.datastore.UserSettings
import com.afterroot.allusive2.datastore.UserSettingsSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@InstallIn(SingletonComponent::class)
@Module
object DataStoreModule {
  @Provides
  @Singleton
  fun providesUserSettingsDataStore(
    @ApplicationContext context: Context,
    dispatchers: CoroutineDispatchers,
    userSettingsSerializer: UserSettingsSerializer,
  ): DataStore<UserSettings> = DataStoreFactory.create(
    serializer = userSettingsSerializer,
    scope = CoroutineScope(SupervisorJob() + dispatchers.io),
  ) {
    context.dataStoreFile("user_settings.pb")
  }
}

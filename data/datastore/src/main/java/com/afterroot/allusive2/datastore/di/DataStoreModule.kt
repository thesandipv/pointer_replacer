/*
 * Copyright (C) 2016-2024 Sandip Vaghela
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

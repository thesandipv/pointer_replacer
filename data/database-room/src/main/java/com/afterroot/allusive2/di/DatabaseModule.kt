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

package com.afterroot.allusive2.di

import android.content.Context
import androidx.room.Room
import app.tivi.data.db.DatabaseTransactionRunner
import app.tivi.data.db.RoomTransactionRunner
import com.afterroot.allusive2.database.MIGRATION_1_2
import com.afterroot.allusive2.database.MyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {
    @Provides
    @Singleton
    fun provideRoomDatabase(
        @ApplicationContext context: Context,
    ): MyDatabase = Room.databaseBuilder(
        context,
        MyDatabase::class.java,
        "installed-pointers",
    ).addMigrations(MIGRATION_1_2).build()

    @Provides
    @Singleton
    fun provideDatabaseTransactionRunner(
        runner: RoomTransactionRunner,
    ): DatabaseTransactionRunner = runner
}

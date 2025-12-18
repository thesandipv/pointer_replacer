/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
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
  fun provideRoomDatabase(@ApplicationContext context: Context): MyDatabase = Room.databaseBuilder(
    context,
    MyDatabase::class.java,
    "installed-pointers",
  ).addMigrations(MIGRATION_1_2).build()

  @Provides
  @Singleton
  fun provideDatabaseTransactionRunner(runner: RoomTransactionRunner): DatabaseTransactionRunner =
    runner
}

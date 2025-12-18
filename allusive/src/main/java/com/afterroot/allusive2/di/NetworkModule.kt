/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.di

import android.content.Context
import android.net.ConnectivityManager
import com.afterroot.utils.network.NetworkStateMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

  @Provides
  @Singleton
  fun provideCM(@ApplicationContext context: Context): ConnectivityManager =
    context.applicationContext.getSystemService(
      Context.CONNECTIVITY_SERVICE,
    ) as ConnectivityManager

  @Provides
  @Singleton
  fun provideStateMonitor(connectivityManager: ConnectivityManager) =
    NetworkStateMonitor(connectivityManager)
}

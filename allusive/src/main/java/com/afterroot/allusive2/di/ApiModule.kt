/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.di

import com.afterroot.allusive2.utils.whenBuildIs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
  @Provides
  @Singleton
  fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .client(okHttpClient)
    .build()

  @Provides
  fun provideOkHttpClient(httpLoggingInterceptor: HttpLoggingInterceptor) =
    OkHttpClient().newBuilder()
      .addInterceptor(httpLoggingInterceptor)
      .build()

  @Provides
  @Singleton
  fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
    level = whenBuildIs(
      debug = HttpLoggingInterceptor.Level.BODY,
      release = HttpLoggingInterceptor.Level.NONE,
    )
  }
}

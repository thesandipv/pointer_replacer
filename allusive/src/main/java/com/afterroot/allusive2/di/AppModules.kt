/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.di

import com.afterroot.allusive2.base.BuildConfig
import com.afterroot.allusive2.base.CoroutineDispatchers
import com.afterroot.allusive2.utils.getMailBodyForFeedback
import com.afterroot.data.utils.FirebaseUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object AppModules {
  @Singleton
  @Provides
  fun provideDispatchers() = CoroutineDispatchers(
    default = Dispatchers.Default,
    io = Dispatchers.IO,
    main = Dispatchers.Main,
    databaseWrite = Dispatchers.IO.limitedParallelism(1),
    databaseRead = Dispatchers.IO.limitedParallelism(4),
  )

  @Provides
  @Named("feedback_email")
  fun provideFeedbackEmail(): String = "afterhasroot@gmail.com"

  @Provides
  @Named("version_Code")
  fun provideVersionCode(): Int = BuildConfig.VERSION_CODE

  @Provides
  @Named("version_name")
  fun provideVersionName(): String = BuildConfig.VERSION_NAME

  @Provides
  @Named("version_string")
  fun provideVersionString() =
    "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) - ${BuildConfig.COMMIT_ID}"

  @Provides
  @Named("feedback_body")
  fun provideFeedbackBody(firebaseUtils: FirebaseUtils): String = getMailBodyForFeedback(
    firebaseUtils,
    version = provideVersionName(),
    versionCode = provideVersionCode(),
  )

  @Provides
  @Singleton
  fun provideGson(): Gson = GsonBuilder().create()
}

/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.di

import app.tivi.util.Logger
import com.afterroot.allusive2.data.BuildConfig
import com.afterroot.allusive2.repository.UserDataRepository
import com.afterroot.allusive2.utils.whenBuildIs
import com.afterroot.data.utils.utils
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.messaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private const val EMULATOR_HOST = "10.0.2.2"
private const val EMULATOR_PORT_FIRESTORE = 8080
private const val EMULATOR_PORT_AUTH = 9099
private const val EMULATOR_PORT_STORAGE = 9199

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
  @Provides
  @Singleton
  fun provideFirestore(userDataRepository: UserDataRepository, logger: Logger): FirebaseFirestore =
    Firebase.firestore.apply {
      val isEmulatorEnabled = runBlocking {
        BuildConfig.DEBUG && userDataRepository.userData.first().enableFirebaseEmulators
      }
      if (isEmulatorEnabled) {
        useEmulator(EMULATOR_HOST, EMULATOR_PORT_FIRESTORE)
        logger.d { "Using firebase emulators: firestore" }
      } else {
        logger.d { "Not Using firebase emulators: firestore" }
      }
      firestoreSettings = firestoreSettings {
        setLocalCacheSettings(
          if (isEmulatorEnabled) {
            MemoryCacheSettings.newBuilder().build()
          } else {
            PersistentCacheSettings.newBuilder().build()
          },
        )
      }
    }

  @Provides
  @Singleton
  fun provideDatabase(): FirebaseDatabase = Firebase.database

  @Provides
  @Singleton
  fun provideAuth(userDataRepository: UserDataRepository, logger: Logger): FirebaseAuth =
    Firebase.auth.apply {
      runBlocking {
        val userData = userDataRepository.userData.first()
        if (BuildConfig.DEBUG && userData.enableFirebaseEmulators) {
          useEmulator(EMULATOR_HOST, EMULATOR_PORT_AUTH)
          logger.d { "Using firebase emulators: auth" }
        } else {
          logger.d { "Not Using firebase emulators: auth" }
        }
      }
    }

  @Provides
  @Singleton
  fun provideStorage(userDataRepository: UserDataRepository, logger: Logger): FirebaseStorage =
    Firebase.storage.apply {
      runBlocking {
        val userData = userDataRepository.userData.first()
        if (BuildConfig.DEBUG && userData.enableFirebaseEmulators) {
          useEmulator(EMULATOR_HOST, EMULATOR_PORT_STORAGE)
          logger.d { "Using firebase emulators: storage" }
        } else {
          logger.d { "Not Using firebase emulators: storage" }
        }
      }
    }

  @Provides
  @Singleton
  fun provideRemoteConfig(): FirebaseRemoteConfig = Firebase.remoteConfig.apply {
    setConfigSettingsAsync(
      remoteConfigSettings {
        fetchTimeoutInSeconds = whenBuildIs(debug = 0, release = 3600)
      },
    )
//        setDefaultsAsync(R.xml.remote_configs)
  }

  @Provides
  @Singleton
  fun provideFirebaseUtils(firebaseAuth: FirebaseAuth) = Firebase.utils(firebaseAuth)

  @Provides
  @Singleton
  fun provideFirebaseMessaging(): FirebaseMessaging = Firebase.messaging

  @Provides
  @Singleton
  fun provideAnalytics(): FirebaseAnalytics = Firebase.analytics

  @Provides
  @Singleton
  fun provideFirebaseCrashlytics(): FirebaseCrashlytics = Firebase.crashlytics
}

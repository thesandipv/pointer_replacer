/*
 * Copyright (C) 2016-2022 Sandip Vaghela
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
  @Provides
  @Singleton
    fun provideFirestore(
        userDataRepository: UserDataRepository,
        logger: Logger,
    ): FirebaseFirestore = Firebase.firestore.apply {
        firestoreSettings = firestoreSettings {
      setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
    }
        runBlocking {
            launch {
                val userData = userDataRepository.userData.first()
                if (BuildConfig.DEBUG && userData.enableFirebaseEmulators) {
                    useEmulator("10.0.2.2", 8080)
                    logger.d { "Using firebase emulators: firestore" }
                }
            }
        }
  }

  @Provides
  @Singleton
  fun provideDatabase(): FirebaseDatabase = Firebase.database

  @Provides
  @Singleton
  fun provideAuth(): FirebaseAuth = Firebase.auth

  @Provides
  @Singleton
    fun provideStorage(
        userDataRepository: UserDataRepository,
        logger: Logger,
    ): FirebaseStorage = Firebase.storage.apply {
        runBlocking {
            launch {
                val userData = userDataRepository.userData.first()
                if (BuildConfig.DEBUG && userData.enableFirebaseEmulators) {
                    useEmulator("10.0.2.2", 9199)
                    logger.d { "Using firebase emulators: storage" }
                }
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

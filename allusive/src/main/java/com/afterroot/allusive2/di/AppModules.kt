/*
 * Copyright (C) 2016-2021 Sandip Vaghela
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
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.afterroot.allusive2.BuildConfig
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.database.roomModule
import com.afterroot.allusive2.utils.FirebaseUtils
import com.afterroot.allusive2.utils.whenBuildIs
import com.afterroot.allusive2.viewmodel.NetworkViewModel
import com.afterroot.core.network.NetworkStateMonitor
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.firestoreSettings
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module

val appModule = module {

    single(qualifier("version_code")) {
        BuildConfig.VERSION_CODE
    }

    single(qualifier("version_name")) {
        BuildConfig.VERSION_NAME
    }
}

val firebaseModule = module {
    single {
        Firebase.firestore.apply {
            firestoreSettings = firestoreSettings {
                isPersistenceEnabled = true
            }
        }
    }

    single { FirebaseStorage.getInstance() }

    single { Firebase.auth }

    single { Firebase.database }

    single {
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(
                remoteConfigSettings {
                    fetchTimeoutInSeconds = whenBuildIs(debug = 0, release = 3600)
                }
            )
        }
    }

    single { FirebaseMessaging.getInstance() }

    single {
        FirebaseUtils(get())
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
val networkModule = module {
    single {
        NetworkStateMonitor(get())
    }

    single {
        androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    viewModel {
        NetworkViewModel(get())
    }
}

val settingsModule = module {
    single {
        Settings(androidContext())
    }
}

val allModules = appModule + firebaseModule + roomModule + settingsModule + networkModule

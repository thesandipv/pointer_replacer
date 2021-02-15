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

package com.afterroot.allusive2

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import com.afterroot.allusive2.database.roomModule
import com.afterroot.core.network.NetworkStateMonitor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module

val appInitModule = module {
    single { AppInitializer.getInstance(androidContext()) }
    single { Settings(androidContext()) }
}

val firebaseModule = module {
    single {
        get<AppInitializer>().initializeComponent(FirestoreInitializer::class.java)
    }

    single {
        FirebaseStorage.getInstance()
    }

    single {
        get<AppInitializer>().initializeComponent(AuthInitializer::class.java)
    }

    single {
        get<AppInitializer>().initializeComponent(ConfigInitializer::class.java)
    }

    single {
        get<AppInitializer>().initializeComponent(MessagingInitializer::class.java)
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
}

@Suppress("unused")
@Keep
class KoinInitializer : Initializer<KoinApplication> {
    override fun create(context: Context): KoinApplication = startKoin {
        androidLogger(Level.ERROR) //TODO Tmp Workaround for Kotlin 1.4
        androidContext(context)
        val modulesList = mutableListOf(firebaseModule, appInitModule, roomModule)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            modulesList.add(networkModule)
        }
        modules(modulesList)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

class FirestoreInitializer : Initializer<FirebaseFirestore> {
    override fun create(context: Context): FirebaseFirestore = Firebase.firestore.apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

class AuthInitializer : Initializer<FirebaseAuth> {
    override fun create(context: Context): FirebaseAuth = FirebaseAuth.getInstance()
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

class ConfigInitializer : Initializer<FirebaseRemoteConfig> {
    override fun create(context: Context): FirebaseRemoteConfig = Firebase.remoteConfig.apply {
        setDefaultsAsync(R.xml.firebase_remote_defaults)
        setConfigSettingsAsync(
            remoteConfigSettings {
                fetchTimeoutInSeconds = if (BuildConfig.DEBUG) 0 else 3600
            }
        )
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

class MessagingInitializer : Initializer<FirebaseMessaging> {
    override fun create(context: Context): FirebaseMessaging = FirebaseMessaging.getInstance()
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
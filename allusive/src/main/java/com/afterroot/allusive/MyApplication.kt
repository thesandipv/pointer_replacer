/*
 * Copyright (C) 2016-2020 Sandip Vaghela
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

package com.afterroot.allusive

import android.os.Build
import androidx.multidex.MultiDexApplication
import com.afterroot.allusive.di.appModule
import com.afterroot.allusive.di.firebaseModule
import com.afterroot.allusive.di.roomModule
import com.afterroot.core.onVersionGreaterThanEqualTo
import fr.dasilvacampos.network.monitoring.NetworkStateHolder.registerConnectivityBroadcaster
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MyApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        startKoin {
            androidLogger()
            androidContext(this@MyApplication)
            modules(listOf(roomModule, firebaseModule, appModule))
        }
        onVersionGreaterThanEqualTo(Build.VERSION_CODES.LOLLIPOP, {
            registerConnectivityBroadcaster()
        })
    }
}
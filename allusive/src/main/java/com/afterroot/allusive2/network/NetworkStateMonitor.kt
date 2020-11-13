/*
 * Copyright (C) 2020 Sandip Vaghela
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

package com.afterroot.allusive2.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
val networkModule = module {
    single {
        NetworkStateMonitor(get())
    }

    single {
        androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class NetworkStateMonitor(private val cm: ConnectivityManager) : LiveData<NetworkState>() {

    private var hasNetworkChanged: Boolean = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            super.onLost(network)
            hasNetworkChanged = true
            postValue(NetworkState.CONNECTION_LOST)
        }

        override fun onUnavailable() {
            super.onUnavailable()
            hasNetworkChanged = true
            postValue(NetworkState.DISCONNECTED)
        }

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            if (hasNetworkChanged) {
                hasNetworkChanged = false
                postValue(NetworkState.CONNECTED)
            }
        }
    }

    override fun onInactive() {
        super.onInactive()
        cm.unregisterNetworkCallback(networkCallback)
    }

    @SuppressLint("MissingPermission")
    override fun onActive() {
        super.onActive()
        cm.registerNetworkCallback(networkRequest(), networkCallback)
    }

    private fun networkRequest(): NetworkRequest {
        return NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
    }
}


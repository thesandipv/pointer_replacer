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

package com.afterroot.allusive2.viewmodel

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.afterroot.core.network.NetworkState
import com.afterroot.core.network.NetworkStateMonitor
import org.koin.core.KoinComponent
import org.koin.core.inject

class NetworkViewModel : ViewModel(), KoinComponent {
    val networkMonitor: LiveData<NetworkState> by inject<NetworkStateMonitor>()

    inline fun monitor(
        lifecycleOwner: LifecycleOwner,
        noinline doInitially: (() -> Unit)? = null,
        crossinline onConnect: (state: NetworkState) -> Unit,
        noinline onDisconnect: ((state: NetworkState) -> Unit)? = null
    ) {
        if (doInitially == null) {
            onConnect(NetworkState.CONNECTED) // Run [doWhenConnected] id [doInitially] is null
        } else {
            doInitially.invoke()
        }
        this.networkMonitor.observe(lifecycleOwner, {
            when (it) {
                NetworkState.CONNECTED -> {
                    onConnect(NetworkState.CONNECTED)
                }
                else -> {
                    onDisconnect?.invoke(it)
                }
            }
        })
    }
}
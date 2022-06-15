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
import androidx.lifecycle.ViewModel
import com.afterroot.utils.network.NetworkState
import com.afterroot.utils.network.NetworkStateMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val networkStateMonitor: NetworkStateMonitor
) : ViewModel() {

    fun monitor(
        lifecycleOwner: LifecycleOwner,
        doInitially: (() -> Unit)? = null,
        onConnect: (state: NetworkState) -> Unit,
        onDisconnect: ((state: NetworkState) -> Unit)? = null
    ) {
        if (doInitially == null) {
            onConnect(NetworkState.CONNECTED) // Run [doWhenConnected] id [doInitially] is null
        } else {
            doInitially.invoke()
        }
        networkStateMonitor.observe(lifecycleOwner) {
            when (it) {
                NetworkState.CONNECTED -> {
                    onConnect(NetworkState.CONNECTED)
                }
                else -> {
                    onDisconnect?.invoke(it)
                }
            }
        }
    }
}

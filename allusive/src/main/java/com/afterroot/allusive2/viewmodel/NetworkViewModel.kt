/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.viewmodel

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.afterroot.utils.network.NetworkState
import com.afterroot.utils.network.NetworkStateMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(private val networkStateMonitor: NetworkStateMonitor) :
  ViewModel() {

  fun monitor(
    lifecycleOwner: LifecycleOwner,
    doInitially: (() -> Unit)? = null,
    onConnect: (state: NetworkState) -> Unit,
    onDisconnect: ((state: NetworkState) -> Unit)? = null,
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

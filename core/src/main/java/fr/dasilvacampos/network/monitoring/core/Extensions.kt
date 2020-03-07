/*
 * Copyright (C) 2016-2019 Sandip Vaghela
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

package fr.dasilvacampos.network.monitoring.core

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import fr.dasilvacampos.network.monitoring.Event
import fr.dasilvacampos.network.monitoring.NetworkConnectivityListener
import fr.dasilvacampos.network.monitoring.NetworkEvents
import fr.dasilvacampos.network.monitoring.NetworkState

internal object Constants {
    const val ID_KEY = "network.monitoring.previousState"
}

internal fun NetworkConnectivityListener.onListenerCreated() {

    NetworkEvents.observe(this as LifecycleOwner, Observer {
        if (previousState != null)
            networkConnectivityChanged(it)
    })

}

internal fun NetworkConnectivityListener.onListenerResume(networkState: NetworkState) {
    if (!shouldBeCalled || !checkOnResume) return

    val previousState = previousState
    val isConnected = networkState.isConnected

    this.previousState = isConnected

    val connectionLost = (previousState == null || previousState == true) && !isConnected
    val connectionBack = previousState == false && isConnected

    if (connectionLost || connectionBack) {
        networkConnectivityChanged(Event.ConnectivityEvent(networkState))
    }

}

/**
 * This property serves as a flag to detect if this activity lost network
 */
internal var NetworkConnectivityListener.previousState: Boolean?
    get() {
        return when (this) {
            is Fragment -> this.arguments?.previousState
            is Activity -> this.intent.extras?.previousState
            else -> null
        }
    }
    set(value) {
        when (this) {
            is Fragment -> {
                val a = this.arguments ?: Bundle()
                a.previousState = value
                this.arguments = a
            }
            is Activity -> {
                val a = this.intent.extras ?: Bundle()
                a.previousState = value
                this.intent.replaceExtras(a)
            }
        }
    }
internal var Bundle.previousState: Boolean?
    get() = when (getInt(Constants.ID_KEY, -1)) {
        -1 -> null
        0 -> false
        else -> true
    }
    set(value) {
        putInt(Constants.ID_KEY, if (value == true) 1 else 0)
    }
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
package com.afterroot.allusive2.utils

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.allusive2.BuildConfig
import com.afterroot.allusive2.R
import com.afterroot.core.network.NetworkState

/**
 * Top Level Misc Functions
 * */

fun Context.showNetworkDialog(
    state: NetworkState,
    positive: () -> Unit,
    negative: () -> Unit,
    isShowHide: Boolean = false
) =
    MaterialDialog(this).show {
        title(text = if (state == NetworkState.CONNECTION_LOST) "Connection Lost" else "Network Disconnected")
        cancelable(false)
        message(R.string.dialog_msg_no_network)
        negativeButton(text = "Exit") {
            negative()
        }
        positiveButton(text = "Retry") {
            positive()
        }
        if (isShowHide) {
            positiveButton(text = "Hide") {
                dismiss()
            }
        }
    }

/**
 * Helper Function for getting different values for Debug and Release builds
 * @param T type of value to return
 * @param debug value to return if build is Debug
 * @param release value to return if build is Release
 * @since v1.9.4
 * @return either [debug] or [release] with provided type [T]
 */
fun <T> whenBuildIs(debug: T, release: T): T = if (BuildConfig.DEBUG) debug else release

/**
 * Helper Function for invoking different functions for Debug and Release builds
 * @param T type of value to return
 * @param debug function to invoke if build is Debug
 * @param release function to invoke if build is Release
 * @since v1.9.4
 * @return either [debug] or [release] with provided type [T]
 */
fun <T> whenBuildIs(debug: () -> T, release: () -> T): T = whenBuildIs(debug.invoke(), release.invoke())

/**
 * Helper Function for invoking function only if build is Debug
 * @param debug function to invoke if build is Debug
 * @since v1.9.4
 */
fun whenBuildIs(debug: () -> Unit) {
    if (BuildConfig.DEBUG) debug.invoke()
}

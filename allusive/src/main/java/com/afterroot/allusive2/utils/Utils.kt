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

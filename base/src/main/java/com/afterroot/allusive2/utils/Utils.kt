/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.utils

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuProvider
import com.afterroot.allusive2.base.BuildConfig
import com.afterroot.utils.network.NetworkState
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Top Level Misc Functions
 * */
/**
 * @return [AlertDialog] based on [NetworkState]
 */
fun Context.showNetworkDialog(
  state: NetworkState,
  positive: () -> Unit,
  negative: () -> Unit,
  isShowHide: Boolean = false,
): AlertDialog {
  val dialog = MaterialAlertDialogBuilder(this).apply {
    setTitle(
      if (state ==
        NetworkState.CONNECTION_LOST
      ) {
        "Connection Lost"
      } else {
        "Network Disconnected"
      },
    )
    setCancelable(false)
    setMessage(com.afterroot.allusive2.resources.R.string.dialog_msg_no_network)
    setNegativeButton("Exit") { _, _ -> negative() }
    if (isShowHide) {
      setPositiveButton("Hide") { dialog, _ -> dialog.dismiss() }
    } else {
      setPositiveButton("Retry") { _, _ -> positive() }
    }
  }
  return dialog.show()
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
fun <T> whenBuildIs(debug: () -> T, release: () -> T): T =
  whenBuildIs(debug.invoke(), release.invoke())

/**
 * Helper Function for invoking function only if build is Debug
 * @param debug function to invoke if build is Debug
 * @since v1.9.4
 */
fun whenBuildIs(debug: () -> Unit) {
  if (BuildConfig.DEBUG) debug.invoke()
}


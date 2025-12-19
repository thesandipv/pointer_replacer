/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber

sealed class InvokeStatus
object InvokeStarted : InvokeStatus()
object InvokeSuccess : InvokeStatus()
data class InvokeError(val throwable: Throwable) : InvokeStatus()

/**
 * Extension function for Watching the [InvokeStatus]
 * @param scope [CoroutineScope] to launch coroutine in.
 * @param onSuccess Lambda to Execute when [InvokeStatus] is [InvokeSuccess]
 * @return Launched [Job] on specified [scope]
 */
fun Flow<InvokeStatus>.watchStatus(
  scope: CoroutineScope,
  tag: String = "",
  onSuccess: () -> Unit = {},
): Job = scope.launch { collectStatus(tag, onSuccess) }

/**
 * Collects [InvokeStatus]
 * @param tag Additional log tag prefix
 * @param onSuccess Lambda to Execute when [InvokeStatus] is [InvokeSuccess]
 */
private suspend fun Flow<InvokeStatus>.collectStatus(
  tag: String = "InvokeStatus",
  onSuccess: () -> Unit = {},
) = collect { status ->

  when (status) {
    InvokeStarted -> {
      Timber.tag(tag).d("collectStatus: Start")
    }
    InvokeSuccess -> {
      Timber.tag(tag).d("collectStatus: Success")
      onSuccess()
    }
    is InvokeError -> {
      Timber.tag(tag).d(status.throwable, "collectStatus: Error")
    }
  }
}

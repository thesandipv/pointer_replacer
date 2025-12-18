/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.base

import kotlinx.coroutines.CoroutineDispatcher

data class CoroutineDispatchers(
  val default: CoroutineDispatcher,
  val io: CoroutineDispatcher,
  val main: CoroutineDispatcher,
  val databaseWrite: CoroutineDispatcher,
  val databaseRead: CoroutineDispatcher,
)

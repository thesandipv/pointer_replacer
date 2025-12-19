/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.utils

sealed class State<T> {
  class Loading<T> : State<T>()
  data class Success<T>(val data: T) : State<T>()
  data class Failed<T>(val message: String) : State<T>()

  companion object {
    fun <T> loading() = Loading<T>()
    fun <T> success(data: T) = Success(data)
    fun <T> failed(message: String) = Failed<T>(message)
  }
}

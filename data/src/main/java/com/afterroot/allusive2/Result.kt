/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2

sealed class Result {
  object Success : Result()
  data class Running(val message: String) : Result()
  data class Failed(val error: String) : Result()
}

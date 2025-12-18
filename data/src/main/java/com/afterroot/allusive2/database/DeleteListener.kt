/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.database

interface DeleteListener {
  fun onDeleteSuccess()
  fun onDeleteFailed()
}

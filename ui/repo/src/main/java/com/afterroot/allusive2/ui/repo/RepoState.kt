/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.ui.repo

import com.afterroot.allusive2.base.compose.ViewState

data class RepoState(val message: String? = null) : ViewState() {
  companion object {
    val Empty = RepoState()
  }
}

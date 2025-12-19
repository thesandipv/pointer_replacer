/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.home

import com.afterroot.allusive2.base.compose.Actions

sealed class HomeActions : Actions() {
  data class LoadIntAd(val isShow: Boolean = false) : HomeActions()
  object ShowIntAd : HomeActions()
  object OnIntAdDismiss : HomeActions()
}

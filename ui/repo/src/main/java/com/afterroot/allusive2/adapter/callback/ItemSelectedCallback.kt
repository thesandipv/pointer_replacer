/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.adapter.callback

import android.view.View

interface ItemSelectedCallback<T> {
  fun onClick(position: Int, view: View? = null) {}
  fun onClick(position: Int, view: View? = null, item: T) {}
  fun onLongClick(position: Int, item: T): Boolean = false
}

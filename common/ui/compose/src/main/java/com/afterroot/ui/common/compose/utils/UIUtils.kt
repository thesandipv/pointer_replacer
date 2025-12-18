/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.ui.common.compose.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CenteredRow(modifier: Modifier = Modifier, content: @Composable (RowScope) -> Unit) {
  Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
    content(this)
  }
}

fun Modifier.bottomNavigationPadding() = this.padding(bottom = 56.dp)

fun Modifier.sidePadding(padding: Dp = 16.dp, applyBottom: Boolean = false) = padding(
  start = padding,
  top = padding,
  end = padding,
  bottom = if (applyBottom) padding else 0.dp,
)

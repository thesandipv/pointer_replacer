/*
 * Copyright (C) 2016-2021 Sandip Vaghela
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

fun Modifier.sidePadding(padding: Dp = 16.dp, applyBottom: Boolean = false) =
    padding(
        start = padding,
        top = padding,
        end = padding,
        bottom = if (applyBottom) padding else 0.dp
    )

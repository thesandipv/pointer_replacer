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
package com.afterroot.ui.common.compose.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Common Top App Bar
 *
 * @param navigationIcon Should be [IconButton]
 * @param actions Should be [IconButton]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonAppBar(
    modifier: Modifier = Modifier,
    withTitle: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = { Text(text = withTitle) },
        navigationIcon = navigationIcon,
        actions = actions
    )
    /* TopAppBar(modifier = modifier) {
         Box(modifier = Modifier.fillMaxWidth()) {
             if (navigationIcon != null) {
                 Row(
                     Modifier
                         .fillMaxHeight()
                         .width(68.dp),
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     CompositionLocalProvider(
                         LocalContentAlpha provides ContentAlpha.high,
                         content = navigationIcon
                     )
                 }
             } else {
                 Spacer(modifier = Modifier.width(12.dp))
             }
             Row(
                 verticalAlignment = Alignment.CenterVertically,
                 horizontalArrangement = Arrangement.Center,
                 modifier = Modifier
                     .fillMaxHeight()
                     .align(Alignment.Center)
             ) {
                 CompositionLocalProvider(
                     LocalContentAlpha provides ContentAlpha.high,
                     content = { Text(text = withTitle, style = appBarTitleStyle) }
                 )
             }

             CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                 Row(
                     Modifier
                         .fillMaxHeight()
                         .fillMaxWidth(),
                     horizontalArrangement = Arrangement.End,
                     verticalAlignment = Alignment.CenterVertically,
                     content = actions
                 )
             }
         }
     }*/
}

@Composable
fun UpActionButton(onUpClick: () -> Unit) {
    IconButton(onClick = onUpClick) {
        Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Back")
    }
}

val AppBarHeight = 56.dp

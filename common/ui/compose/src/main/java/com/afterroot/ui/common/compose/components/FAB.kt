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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.afterroot.ui.common.compose.animation.AnimatedFab
import com.afterroot.ui.common.compose.animation.FabState
import com.afterroot.ui.common.compose.theme.fabShape

@Composable
fun FABAdd(modifier: Modifier = Modifier, onClick: () -> Unit) {
  AnimatedFab { state, scale, alpha ->
    CommonFAB(
      icon = Icons.Rounded.Add,
      modifier = modifier
        .alpha(alpha.value)
        .scale(scale.value),
      onClick = onClick,
    )
    state.value = FabState.Visible
  }
}

@Composable
fun FABDone(modifier: Modifier = Modifier, onClick: () -> Unit) {
  AnimatedFab { state, scale, alpha ->
    CommonFAB(
      icon = Icons.Rounded.Done,
      modifier = modifier
        .alpha(alpha.value)
        .scale(scale.value),
      onClick = onClick,
    )
    state.value = FabState.Visible
  }
}

@Composable
fun FABSave(modifier: Modifier = Modifier, onClick: () -> Unit) {
  AnimatedFab { state, scale, alpha ->
    CommonFAB(
      icon = Icons.Rounded.Save,
      modifier = modifier
        .alpha(alpha.value)
        .scale(scale.value),
      onClick = onClick,
    )
    state.value = FabState.Visible
  }
}

@Composable
fun FABEdit(modifier: Modifier = Modifier, onClick: () -> Unit) {
  AnimatedFab { state, scale, alpha ->
    CommonFAB(
      icon = Icons.Rounded.Edit,
      modifier = modifier
        .alpha(alpha.value)
        .scale(scale.value),
      onClick = onClick,
    )
    state.value = FabState.Visible
  }
}

@Composable
internal fun CommonFAB(icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
  FloatingActionButton(
    onClick = onClick,
    modifier = modifier,
    shape = fabShape,
    contentColor = Color.Black,
  ) {
    Icon(imageVector = icon, contentDescription = icon.name)
  }
}

val FABSize = 56.dp

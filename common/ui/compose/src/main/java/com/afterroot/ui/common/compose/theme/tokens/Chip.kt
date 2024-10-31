/*
 * Copyright (C) 2016-2022 Sandip Vaghela
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
package com.afterroot.ui.common.compose.theme.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afterroot.ui.common.compose.theme.Palette

object Chip {
  val ContainerShape = RoundedCornerShape(8.dp)
  val ContainerHeight = 32.dp
  val ContainerElevation = 0.dp
  val TextChipPaddingStart = 16.dp
  val TextChipPaddingEnd = 16.dp
  val containerStyle: AssistChipContainer = AssistChipContainer()
}

object AssistChip {
  val ContainerShape = RoundedCornerShape(8.dp)
  val ContainerHeight = 32.dp

  // Outline Color = md.sys.color.outline
  val containerStyle: AssistChipContainer = AssistChipContainer()
}

data class AssistChipContainer(
  val elevated: Boolean = false,
  val ContainerElevation: Dp = if (elevated) {
    1.dp // md.sys.elevation.level1
  } else {
    0.dp // md.sys.elevation.level0
  },
  val OutlineSize: Dp = if (elevated) 0.dp else 1.dp,
  val OutlineColor: Color = Palette.NeutralVariant60, // md.sys.color.outline

  // if Elevated tokens
  // Color = md.sys.color.surface
  // Shadow = md.sys.color.shadow
)

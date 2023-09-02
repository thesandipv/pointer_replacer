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
package com.afterroot.ui.common.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afterroot.ui.common.compose.theme.tokens.Chip
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Chip of [String]
 * @param text text to display
 * @param isSelected Show as selected or not
 * @param backgroundColor background color of chip.
 * Defaults to primary color if chip is selected otherwise surface color.
 * @param onSelectionChanged callback to be invoked when a chip is clicked.
 * The lambda carries out [String], of which state is changed.
 */
@Composable
fun TextChip(
    modifier: Modifier = Modifier,
    text: String? = null,
    isSelected: Boolean = false,
    backgroundColor: Color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
    clickable: Boolean = true,
    elevation: Dp = Chip.ContainerElevation,
    shape: Shape = Chip.ContainerShape,
    minHeight: Dp = Chip.ContainerHeight,
    onSelectionChanged: (String) -> Unit = {},
) {
    Surface(
        modifier = Modifier
            .then(modifier)
            .defaultMinSize(minHeight = minHeight),
        // elevation = elevation,
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(Chip.containerStyle.OutlineSize, Chip.containerStyle.OutlineColor),
    ) {
        Row(
            modifier = Modifier
                .toggleable(
                    value = isSelected,
                    enabled = clickable,
                    role = Role.Checkbox,
                    onValueChange = {
                        onSelectionChanged(text ?: "")
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text ?: "",
                modifier = Modifier
                    .padding(start = Chip.TextChipPaddingStart, end = Chip.TextChipPaddingEnd)
                    .placeholder(
                        visible = text.isNullOrBlank(),
                        highlight = PlaceholderHighlight.shimmer(),
                    ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Toggleable ChipGroup of String. Use another overload if selection not required.
 * @param modifier the modifier to apply to this layout
 * @param list the list of String to display
 * @param horizontalSpacing Spacing on both side of ChipGroup
 * @param onSelectedChanged callback to be invoked when a chip is clicked.
 * The lambda carries out two parameters,
 * 1. selected - the [String] that state changed
 * 2. selectedChips - List of Selected [String] objects
 */
@Composable
fun TextChipGroup(
    modifier: Modifier = Modifier,
    chipModifier: Modifier = Modifier,
    horizontalSpacing: Dp = 0.dp,
    list: List<String>? = null,
    elevation: Dp = Chip.ContainerElevation,
    chipShape: Shape = Chip.ContainerShape,
    selectionType: SelectionType = SelectionType.Single,
    onSelectedChanged: (selected: String, selectedChips: List<String>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val selectedChips = MutableStateFlow(listOf<String>())
    val selectedState: State<List<String>> = selectedChips.collectAsState()

    LazyRow(modifier) {
        item {
            Spacer(Modifier.size(horizontalSpacing))
        }
        list?.let { list1 ->
            items(list1) { s ->
                TextChip(
                    modifier = chipModifier,
                    text = s,
                    isSelected = selectedState.value.contains(s),
                    elevation = elevation,
                    shape = chipShape,
                    onSelectionChanged = {
                        val newList = when (selectionType) {
                            SelectionType.Single -> {
                                mutableListOf(it)
                            }
                            SelectionType.Multiple -> {
                                selectedState.value.toMutableList() // Convert to MutableList
                            }
                        }
                        if (!selectedState.value.contains(s)) {
                            newList.add(s)
                            scope.launch {
                                selectedChips.emit(newList)
                            }
                        }
                        onSelectedChanged(it, newList)
                    },
                )
            }
        }
        item {
            Spacer(Modifier.size(horizontalSpacing))
        }
    }
}

enum class SelectionType {
    Single, Multiple
}

/**
 * Not Clickable ChipGroup of String
 * @param modifier the modifier to apply to this layout
 * @param list the list of String to display
 * @param horizontalSpacing Spacing on both side of ChipGroup
 */
@Composable
fun TextChipGroup(
    modifier: Modifier = Modifier,
    chipModifier: Modifier = Modifier,
    horizontalSpacing: Dp = 0.dp,
    elevation: Dp = Chip.ContainerElevation,
    shape: Shape = Chip.ContainerShape,
    list: List<String>? = null,
) {
    LazyRow(modifier) {
        item {
            Spacer(Modifier.size(horizontalSpacing))
        }
        list?.let { list1 ->
            items(list1) { s ->
                TextChip(
                    modifier = chipModifier,
                    text = s,
                    isSelected = false,
                    clickable = false,
                    elevation = elevation,
                    shape = shape,
                )
            }
        }
        item {
            Spacer(Modifier.size(horizontalSpacing))
        }
    }
}

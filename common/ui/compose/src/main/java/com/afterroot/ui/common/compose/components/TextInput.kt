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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * [TextField] with Validation
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@Composable
fun TextInput(
  modifier: Modifier = Modifier,
  label: String,
  maxLines: Int = 1,
  errorText: String = "",
  keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current,
  keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
  keyboardActions: KeyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
  validate: (String) -> Boolean = { true },
  onChange: (String) -> Unit,
  onError: (String) -> Unit = {},
) {
  var value by remember { mutableStateOf("") }
  var error by remember { mutableStateOf(false) }
  Column {
    TextField(
      value = value,
      onValueChange = {
        value = it // always update state
        when {
          validate(it) || it.isBlank() -> {
            error = false
            onChange(it)
          }
          else -> {
            error = true
            onError(it)
          }
        }
      },
      isError = error,
      label = { Text(text = label) },
      maxLines = maxLines,
      modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(top = 8.dp),
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
    )
    AnimatedVisibility(visible = error) {
      Text(
        text = errorText,
        modifier = Modifier
          .paddingFromBaseline(top = 16.dp)
          .padding(horizontal = (16.dp * 2)),
        maxLines = 1,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.labelSmall,
      )
    }
  }
}

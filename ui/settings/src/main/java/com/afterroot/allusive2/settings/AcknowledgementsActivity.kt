/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */

package com.afterroot.allusive2.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.afterroot.allusive2.data.model.OssLibrary
import com.afterroot.allusive2.settings.viewmodel.AcknowledgementsViewModel
import com.afterroot.ui.common.compose.theme.Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AcknowledgementsActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      Theme {
        ProvideTextStyle(LocalTextStyle.current) {
          Acknowledgements(
            onNavigationIconClick = {
              this.finish()
            },
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Acknowledgements(
  modifier: Modifier = Modifier,
  viewModel: AcknowledgementsViewModel = viewModel(),
  onNavigationIconClick: () -> Unit,
) {
  val ossLibraries by viewModel.ossLibraries.collectAsStateWithLifecycle()
  val context = LocalContext.current
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = {
          Text("Open Source Licences")
        },
        navigationIcon = {
          IconButton(onClick = onNavigationIconClick) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Back",
            )
          }
        },
      )
    },
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .consumeWindowInsets(paddingValues)
        .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
      LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      ) {
        items(items = ossLibraries) {
          OssLibraryItem(
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 48.dp),
            name = it.name,
            license = it.license,
          ) {
            it.license?.url?.let { urlString ->
              val webpage = urlString.toUri()
              val intent = Intent(Intent.ACTION_VIEW, webpage)
              if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun OssLibraryItem(
  modifier: Modifier = Modifier,
  name: String,
  license: OssLibrary.License?,
  onClick: () -> Unit,
) {
  ListItem(
    modifier = modifier.clickable(onClick = onClick),
    headlineContent = {
      Text(name)
    },
    supportingContent = {
      license?.name?.let { Text(it) }
    },
  )
}

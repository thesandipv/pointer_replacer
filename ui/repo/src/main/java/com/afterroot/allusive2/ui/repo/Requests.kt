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
package com.afterroot.allusive2.ui.repo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import coil.compose.rememberImagePainter
import com.afterroot.allusive2.Constants
import com.afterroot.allusive2.model.LocalPointerRequest
import com.afterroot.allusive2.viewmodel.RepoViewModel
import com.afterroot.ui.common.compose.theme.Palette
import com.afterroot.ui.common.compose.utils.rememberFlowWithLifecycle
import timber.log.Timber
import com.afterroot.allusive2.resources.R as CommonR

@Composable
fun Requests() {
    Requests(viewModel = hiltViewModel())
}

@Composable
internal fun Requests(viewModel: RepoViewModel) {
    val requestsList = rememberFlowWithLifecycle(flow = viewModel.requestPagedList).collectAsLazyPagingItems()
    Requests(viewModel = viewModel, requestsList = requestsList) { action ->
        when (action) {
            else -> viewModel.submitAction(action)
        }
    }
}

@Composable
fun Requests(
    viewModel: RepoViewModel,
    requestsList: LazyPagingItems<LocalPointerRequest>,
    actions: (RepoActions) -> Unit
) {
    val state by rememberFlowWithLifecycle(flow = viewModel.state).collectAsState(initial = RepoState.Empty)
    val isLoading = requestsList.loadState.refresh == LoadState.Loading
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
        AnimatedVisibility(visible = !isLoading) {
            RequestsList(list = requestsList, onClick = {
                Timber.d("Requests: ${it.fileName}")
            })
        }
    }
}

@Composable
fun RequestsList(list: LazyPagingItems<LocalPointerRequest>, onClick: (LocalPointerRequest) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(list) { item ->
            if (item != null) {
                RequestListItem(pointerRequest = item)
            }
        }

        if (list.loadState.append == LoadState.Loading) {
            item {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestListItem(modifier: Modifier = Modifier, pointerRequest: LocalPointerRequest) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .then(modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(id = CommonR.dimen.activity_horizontal_margin),
                    vertical = 8.dp
                )
        ) {
            Column(
                modifier = Modifier
            ) {
                Text(
                    text = pointerRequest.pointerName.toString(),
                    modifier = Modifier
                )
                Text(
                    text = pointerRequest.fileName.toString(),
                    modifier = Modifier
                )
            }
            Column {
                if (pointerRequest.isRequestClosed) {
                    Text(
                        text = "CLOSED",
                        color = Palette.Green50,
                        modifier = Modifier
                    )
                } else Text(text = "OPEN", color = Palette.Red50, modifier = Modifier)
            }
        }
    }
}

@Composable
fun PointerIcon(url: String) {
    Image(
        painter = rememberImagePainter(data = url, builder = {
            crossfade(true)
        }),
        contentDescription = "Pointer Icon"
    )
}

val placeholderRequests = buildList {
    repeat(8) {
        this.add(
            LocalPointerRequest(
                pointerName = Constants.PLACEHOLDER_3,
                fileName = Constants.PLACEHOLDER_3,
                isRequestClosed = false
            )
        )
    }
}

@Preview
@Composable
fun PreviewRequestListItem() {
    Column {
        RequestListItem(
            pointerRequest = LocalPointerRequest(
                fileName = "test.png",
                pointerName = "Pointer Name",
                isRequestClosed = true
            )
        )
        RequestListItem(
            pointerRequest = LocalPointerRequest(
                pointerName = Constants.PLACEHOLDER_3,
                fileName = Constants.PLACEHOLDER_3,
                isRequestClosed = false
            )
        )
    }
}

@Preview
@Composable
fun PreviewRequestListItemDark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Column {
            RequestListItem(
                pointerRequest = LocalPointerRequest(
                    fileName = "test.png",
                    pointerName = "Pointer Name",
                    isRequestClosed = true
                )
            )
            RequestListItem(
                pointerRequest = LocalPointerRequest(
                    pointerName = Constants.PLACEHOLDER_3,
                    fileName = Constants.PLACEHOLDER_3,
                    isRequestClosed = false
                )
            )
        }
    }
}

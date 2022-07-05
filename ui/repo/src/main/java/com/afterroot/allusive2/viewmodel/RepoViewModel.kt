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
package com.afterroot.allusive2.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.afterroot.allusive2.data.pointers
import com.afterroot.allusive2.data.requests
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.domain.interactors.PagingPointerRequest
import com.afterroot.allusive2.model.LocalPointerRequest
import com.afterroot.allusive2.ui.repo.RepoActions
import com.afterroot.allusive2.ui.repo.RepoState
import com.afterroot.data.model.UserRole
import com.afterroot.data.utils.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RepoViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val pagingPointerRequest: PagingPointerRequest,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val firebaseUtils: FirebaseUtils
) : ViewModel() {

    private val actions = MutableSharedFlow<RepoActions>()
    val requestPagedList: Flow<PagingData<LocalPointerRequest>> = pagingPointerRequest.flow.cachedIn(viewModelScope)
    private val messages = MutableSharedFlow<String>()

    val state: StateFlow<RepoState> = combine(messages) {
        RepoState(it[0])
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RepoState.Empty
    )

    init {
        Timber.d("init")

        viewModelScope.launch {
            actions.collect { action ->
                when (action) {
                    RepoActions.LoadRequests -> loadRequests()
                    is RepoActions.ApproveRequest -> {
                    }
                    else -> {}
                }
            }
        }
    }

    internal fun submitAction(action: RepoActions) {
        viewModelScope.launch {
            actions.emit(action)
        }
    }

    private fun loadRequests() {
        val baseQuery = firestore.requests().orderBy(DatabaseFields.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
        var query = baseQuery.whereEqualTo(DatabaseFields.FIELD_UID, firebaseUtils.uid)
        if (firebaseUtils.networkUser?.properties?.userRole == UserRole.ADMIN) {
            query = baseQuery
        }
        pagingPointerRequest(PagingPointerRequest.Params(query, firestore, pagingConfig = PAGING_CONFIG))
    }

    private suspend fun getDownloadUrl(fileName: String): String {
        return storage.pointers().child(fileName).downloadUrl.await().toString()
    }

    companion object {
        private val PAGING_CONFIG = PagingConfig(
            pageSize = 20,
            initialLoadSize = 20,
        )
    }
}

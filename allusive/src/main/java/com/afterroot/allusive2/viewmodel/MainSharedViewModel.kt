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
package com.afterroot.allusive2.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.data.FirestorePagingSource
import com.afterroot.data.utils.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainSharedViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val remoteConfig: FirebaseRemoteConfig,
    private val firebaseFirestore: FirebaseFirestore,
    private val firebaseUtils: FirebaseUtils,
    private val settings: Settings
) : ViewModel() {
    private val _snackbarMsg = MutableLiveData<Event<String>>()
    val liveTitle = MutableLiveData<String>()

    init {
        remoteConfig.fetchAndActivate().addOnSuccessListener {
            savedStateHandle["configLoaded"] = true
        }
    }

    val pointers = Pager(PagingConfig(20)) {
        FirestorePagingSource(firebaseFirestore, settings, firebaseUtils)
    }.flow.cachedIn(viewModelScope)

    fun setTitle(title: String?) {
        if (liveTitle.value != title) { // Don't change title if new title is equal to old.
            liveTitle.value = title!!
        }
    }

    val snackbarMsg: LiveData<Event<String>>
        get() = _snackbarMsg

    fun displayMsg(msg: String) {
        _snackbarMsg.value = Event(msg)
    }
}

/*
 * Copyright (C) 2016-2019 Sandip Vaghela
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

package com.afterroot.allusive.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.afterroot.allusive.database.DatabaseFields
import com.afterroot.allusive.database.dbInstance
import com.google.firebase.firestore.Query

class PointerRepoViewModel : ViewModel() {
    private var pointerSnapshot = MutableLiveData<ViewModelState>()

    fun getPointerSnapshot(): LiveData<ViewModelState> {
        if (pointerSnapshot.value == null) {
            pointerSnapshot.postValue(ViewModelState.Loading)
            dbInstance.collection(DatabaseFields.COLLECTION_POINTERS)
                .orderBy(DatabaseFields.FIELD_TIME, Query.Direction.DESCENDING)
                .addSnapshotListener { querySnapshot, _ ->
                    if (querySnapshot != null) {
                        pointerSnapshot.postValue(ViewModelState.Loaded(querySnapshot))
                    }
                }
        }
        return pointerSnapshot
    }
}
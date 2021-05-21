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

package com.afterroot.allusive2.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.model.Pointer
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await

class FirestorePagingSource(private val firestore: FirebaseFirestore) : PagingSource<QuerySnapshot, Pointer>() {
    override fun getRefreshKey(state: PagingState<QuerySnapshot, Pointer>): QuerySnapshot? {
        TODO("Not yet implemented")
    }

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Pointer> {
        return try {
            val currentPage: QuerySnapshot =
                params.key ?: firestore.collection(DatabaseFields.COLLECTION_POINTERS).limit(10).get().await()

            val lastDocumentSnapshot = currentPage.documents[currentPage.size() - 1]

            val nextPage = firestore.collection(DatabaseFields.COLLECTION_POINTERS)
                .limit(10).startAfter(lastDocumentSnapshot)
                .get()
                .await()

            LoadResult.Page(
                data = currentPage.toObjects(Pointer::class.java),
                prevKey = null,
                nextKey = nextPage
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
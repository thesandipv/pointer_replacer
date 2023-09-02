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
package com.afterroot.allusive2.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.afterroot.allusive2.data.mapper.toLocalPointerRequest
import com.afterroot.allusive2.data.mapper.toRequest
import com.afterroot.allusive2.data.mapper.toRequests
import com.afterroot.allusive2.model.LocalPointerRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class PointerRequestsPagingSource(private val query: Query, private val firestore: FirebaseFirestore) : PagingSource<QuerySnapshot, LocalPointerRequest>() {
    override fun getRefreshKey(
        state: PagingState<QuerySnapshot, LocalPointerRequest>,
    ): QuerySnapshot? {
        return null
    }

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, LocalPointerRequest> {
        return try {
            var currentPageSource = Source.DEFAULT
            var nextPageSource = Source.DEFAULT

            val cachedSnapshot = query.limit(3).get(Source.CACHE).await()

            if (!cachedSnapshot.isEmpty && cachedSnapshot.size() > 2) {
                val latestSnapshot = query.limit(1).get().await()
                val latest = latestSnapshot.documents.first().toRequest()
                val cached = cachedSnapshot.documents.first().toRequest()
                Timber.d("load: Latest - ${latest?.fileName}")
                Timber.d("load: Cached - ${cached?.fileName}")
                Timber.d("load: isSame - ${latest == cached}")

                if (latest != cached) {
                    currentPageSource = Source.DEFAULT
                }
            } else {
                currentPageSource = Source.DEFAULT
            }

            Timber.d("load: Current Page Source: ${currentPageSource.name}")
            var currentPage = params.key ?: query.limit(20).get(currentPageSource).await()

            if (currentPage.isEmpty || currentPage.size() < 15) {
                Timber.d("load: Cache is empty. Getting data from Server.")
                currentPage = params.key ?: query.limit(20).get().await()
            }

            val nextPageQuery = query
                .limit(15)
                .startAfter(currentPage.documents.last())

            Timber.d("load: Next Page Source: ${nextPageSource.name}")
            val nextPage = nextPageQuery.get(nextPageSource).await()

            LoadResult.Page(
                data = currentPage.toRequests().toLocalPointerRequest(firestore),
                prevKey = null,
                nextKey = nextPage,
            )
        } catch (e: Exception) {
            Timber.e(e, "load: Error while loading")
            LoadResult.Error(e)
        }
    }
}

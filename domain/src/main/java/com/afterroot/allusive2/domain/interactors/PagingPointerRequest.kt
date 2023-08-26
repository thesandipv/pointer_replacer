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

package com.afterroot.allusive2.domain.interactors

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.afterroot.allusive2.data.PointerRequestsPagingSource
import com.afterroot.allusive2.domain.PagingInteractor
import com.afterroot.allusive2.model.LocalPointerRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class PagingPointerRequest @Inject constructor() : PagingInteractor<PagingPointerRequest.Params, LocalPointerRequest>() {

    data class Params(
        val query: Query,
        val firestore: FirebaseFirestore,
        val cached: Boolean = false,
        override val pagingConfig: PagingConfig
    ) : Parameters<LocalPointerRequest>

    override fun createObservable(params: Params): Flow<PagingData<LocalPointerRequest>> = Pager(
        config = params.pagingConfig
    ) {
        PointerRequestsPagingSource(params.query, params.firestore)
    }.flow
}

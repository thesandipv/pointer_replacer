/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */

package com.afterroot.allusive2.domain.interactors

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import app.tivi.domain.PagingInteractor
import com.afterroot.allusive2.data.PointerRequestsPagingSource
import com.afterroot.allusive2.model.LocalPointerRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class PagingPointerRequest @Inject constructor(private val firestore: FirebaseFirestore) :
  PagingInteractor<PagingPointerRequest.Params, LocalPointerRequest>() {

  data class Params(
    val query: Query,
    val cached: Boolean = false,
    override val pagingConfig: PagingConfig,
  ) : Parameters<LocalPointerRequest>

  override suspend fun createObservable(params: Params): Flow<PagingData<LocalPointerRequest>> =
    Pager(
      config = params.pagingConfig,
    ) {
      PointerRequestsPagingSource(params.query, firestore)
    }.flow
}

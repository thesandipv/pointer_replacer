/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.afterroot.allusive2.PointerStatus
import com.afterroot.allusive2.Reason
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.data.mapper.toPointer
import com.afterroot.allusive2.data.mapper.toPointers
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.model.Pointer
import com.afterroot.data.utils.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class FirestorePagingSource(
  private val firestore: FirebaseFirestore,
  private val settings: Settings,
  private val firebaseUtils: FirebaseUtils,
) : PagingSource<QuerySnapshot, Pointer>() {
  override fun getRefreshKey(state: PagingState<QuerySnapshot, Pointer>): QuerySnapshot? = null

  override suspend fun load(
    params: LoadParams<QuerySnapshot>,
  ): LoadResult<QuerySnapshot, Pointer> = try {
    var pointersQuery = firestore.pointers()
      .orderBy(settings.orderBy, Query.Direction.DESCENDING)

    if (settings.filterUserPointers) {
      pointersQuery = firestore.pointers()
        .orderBy("${DatabaseFields.FIELD_UPLOAD_BY}.${firebaseUtils.uid}")
        // .orderBy(settings.orderBy, Query.Direction.DESCENDING)
        .whereNotEqualTo("${DatabaseFields.FIELD_UPLOAD_BY}.${firebaseUtils.uid}", "")
    }

    if (settings.filterRRO) {
      pointersQuery = pointersQuery.whereEqualTo(DatabaseFields.FIELD_HAS_RRO, true)
    }

    var currentPageSource = if (settings.orderBy == DatabaseFields.FIELD_DOWNLOADS ||
      settings.filterRRO
    ) {
      Source.DEFAULT
    } else {
      Source.CACHE
    }
    var nextPageSource = Source.DEFAULT

    val cachedPointerSnapshot = pointersQuery.limit(3).get(Source.CACHE).await()

    if (!cachedPointerSnapshot.isEmpty && cachedPointerSnapshot.size() > 2) {
      val latestPointerSnapshot = pointersQuery.limit(1).get().await()
      val latestPointer = latestPointerSnapshot.documents.first().toPointer()
      val cachedPointer = cachedPointerSnapshot.documents.first().toPointer()
      Timber.d("load: Latest Pointer - ${latestPointer?.name}")
      Timber.d("load: Cached Pointer - ${cachedPointer?.name}")
      Timber.d("load: Is Pointers Same: ${latestPointer == cachedPointer}")

      if (latestPointer != cachedPointer) {
        currentPageSource = Source.DEFAULT
      }
    } else {
      currentPageSource = Source.DEFAULT
    }

    Timber.d("load: Source: currentPage loading source: ${currentPageSource.name}")

    var currentPage: QuerySnapshot =
      params.key ?: pointersQuery.limit(20).get(currentPageSource).await()

    if (currentPage.isEmpty || currentPage.size() < 15) {
      Timber.d("load: Cache is empty. Getting data from Server.")
      currentPage = params.key ?: pointersQuery.limit(20).get().await()
    }

    val nextPageQuery = pointersQuery
      .limit(15)
      .startAfter(currentPage.documents.last())

    // Server query 1st pointer and Cached query 1st pointer
            /*val nextPointerCompareQuery = pointersQuery.limit(1).startAfter(currentPage.documents.last().toPointer())
            val latestPointerNextSnapshot = nextPointerCompareQuery.get().await()
            val cachedPointerNextSnapshot = nextPointerCompareQuery.get(Source.CACHE).await()
            val latestPointerNext = latestPointerNextSnapshot.documents.first().toPointer()
            val cachedPointerNext = cachedPointerNextSnapshot.documents.first().toPointer()
            Timber.d("load: Latest PointerNext - ${latestPointerNext?.name}")
            Timber.d("load: Cached PointerNext - ${cachedPointerNext?.name}")
            Timber.d("load: Is NextPointers Same: ${latestPointerNext == cachedPointerNext}")

            if (latestPointerNext != cachedPointerNext) {
                nextPageSource = Source.DEFAULT
            }*/

    Timber.d("load: Source: nextPage loading source: ${nextPageSource.name}")

    val nextPage = nextPageQuery.get(nextPageSource).await()

    val activePointers = currentPage.toPointers().filterNotNull().filter { pointer ->
      val isOwner = pointer.uploadedBy?.containsKey(firebaseUtils.uid) == true
      if (isOwner) {
        pointer.status != PointerStatus.DELETED
      } else {
        (pointer.status == PointerStatus.APPROVED || pointer.status.isEmpty()) &&
          pointer.reasonCode == Reason.OK
      }
    }

    LoadResult.Page(
      data = activePointers,
      prevKey = null,
      nextKey = nextPage,
    )
  } catch (e: Exception) {
    Timber.e(e, "load: Error while loading")
    LoadResult.Error(e)
  }
}

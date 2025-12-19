/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.data.mapper

import com.afterroot.allusive2.data.pointers
import com.afterroot.allusive2.model.LocalPointerRequest
import com.afterroot.allusive2.model.PointerRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import timber.log.Timber

fun QuerySnapshot.toRequests(): List<PointerRequest?> = this.documents.map { it.toRequest() }

fun DocumentSnapshot.toRequest(): PointerRequest? = toObject(PointerRequest::class.java)

fun PointerRequest.toLocalPointerRequest() = LocalPointerRequest(
  fileName,
  uid,
  timestamp,
  force,
  exclude,
  documentId,
  isRequestClosed,
)

suspend fun List<PointerRequest?>.toLocalPointerRequest(
  firestore: FirebaseFirestore,
): List<LocalPointerRequest> {
  val list = mutableListOf<LocalPointerRequest>()
  this.map { pointerRequest ->
    if (pointerRequest?.documentId != null) {
      val query = firestore.pointers().document(pointerRequest.documentId!!)
      var result = kotlin.runCatching {
        query.get(Source.CACHE).await()
      }
      if (result.isFailure) {
        Timber.d("toLocalPointerRequest: Getting info from firestore.")
        result = kotlin.runCatching {
          query.get(Source.DEFAULT).await()
        }
      }
      if (result.isSuccess) {
        result.getOrNull()?.toPointer().let {
          val transformed = pointerRequest.toLocalPointerRequest().copy(
            pointerName = it?.name,
          )
          list.add(transformed)
        }
      }
    }
  }
  return list
}

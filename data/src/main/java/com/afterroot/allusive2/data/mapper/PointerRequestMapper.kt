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

fun QuerySnapshot.toRequests(): List<PointerRequest?> {
    return this.documents.map { it.toRequest() }
}

fun DocumentSnapshot.toRequest(): PointerRequest? = toObject(PointerRequest::class.java)

fun PointerRequest.toLocalPointerRequest() = LocalPointerRequest(
    fileName, uid, timestamp, force, exclude, documentId, isRequestClosed
)

suspend fun List<PointerRequest?>.toLocalPointerRequest(firestore: FirebaseFirestore): List<LocalPointerRequest> {
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
                    val transformed = pointerRequest.toLocalPointerRequest().copy(pointerName = it?.name)
                    list.add(transformed)
                }
            }
        }
    }
    return list
}

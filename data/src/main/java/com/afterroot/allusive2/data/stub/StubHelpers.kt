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
package com.afterroot.allusive2.data.stub

import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.model.Pointer
import com.afterroot.data.utils.FirebaseUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

fun createStubPointers(firestore: FirebaseFirestore, firebaseUtils: FirebaseUtils) {
  repeat(50) { i ->
    val pointer = Pointer(
      name = "Pointer $i",
      filename = "pointer$i.png",
      description = "Awesome Pointer $i",
      uploadedBy = hashMapOf(Pair(firebaseUtils.uid ?: "stub-uid", "Awesome User")),
      time = Timestamp.now().toDate(),
    )
    firestore.collection(DatabaseFields.COLLECTION_POINTERS).document().set(pointer)
  }
}

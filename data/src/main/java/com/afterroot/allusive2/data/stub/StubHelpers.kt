/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
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

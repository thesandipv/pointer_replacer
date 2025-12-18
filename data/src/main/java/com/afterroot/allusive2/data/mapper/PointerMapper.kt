/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.data.mapper

import com.afterroot.allusive2.model.Pointer
import com.afterroot.allusive2.model.RoomPointer
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

fun Pointer.toRoomPointer(): RoomPointer {
  var uploaderId = ""
  var uploaderName = ""
  uploadedBy?.forEach {
    uploaderId = it.key
    uploaderName = it.value
  }
  return RoomPointer(
    file_name = filename,
    pointer_desc = description,
    pointer_name = name,
    uploader_id = uploaderId,
    uploader_name = uploaderName,
  )
}

fun QuerySnapshot.toPointers(): List<Pointer?> = this.documents.map { it.toPointer() }

fun DocumentSnapshot.toPointer(): Pointer? = toObject(Pointer::class.java)?.copy(docId = id)

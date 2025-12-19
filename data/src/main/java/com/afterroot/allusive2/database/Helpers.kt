/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.database

import com.afterroot.allusive2.model.RoomPointer

suspend fun MyDatabase.addLocalPointer(fileName: String) {
  val pointer = RoomPointer(
    file_name = fileName,
    uploader_name = "You (Local)",
    uploader_id = "N/A",
    pointer_name = fileName,
    pointer_desc = "N/A",
  )
  addRoomPointer(pointer)
}

suspend fun MyDatabase.addRoomPointer(roomPointer: RoomPointer) {
  if (roomPointer.file_name == null) {
    return
  }
  if (pointerDao().exists(roomPointer.file_name).isEmpty()) {
    pointerDao().add(roomPointer)
  }
}

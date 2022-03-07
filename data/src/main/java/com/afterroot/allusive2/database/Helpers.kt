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
package com.afterroot.allusive2.database

import com.afterroot.allusive2.model.RoomPointer

suspend fun MyDatabase.addLocalPointer(fileName: String) {
    val pointer = RoomPointer(
        file_name = fileName,
        uploader_name = "You (Local)",
        uploader_id = "N/A",
        pointer_name = fileName,
        pointer_desc = "N/A"
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

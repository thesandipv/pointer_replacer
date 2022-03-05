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

import com.afterroot.allusive2.model.Pointer
import com.afterroot.allusive2.model.RoomPointer
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

fun Pointer.toRoomPointer(): RoomPointer {
    var id = ""
    var name = ""
    uploadedBy?.forEach {
        id = it.key
        name = it.value
    }
    return RoomPointer(
        file_name = filename,
        pointer_desc = description,
        pointer_name = name,
        uploader_id = id,
        uploader_name = name
    )
}

fun QuerySnapshot.toPointers(): List<Pointer> = toObjects(Pointer::class.java)
fun DocumentSnapshot.toPointer(): Pointer? = toObject(Pointer::class.java)

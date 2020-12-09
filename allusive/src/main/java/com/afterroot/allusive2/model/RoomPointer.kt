/*
 * Copyright (C) 2016-2020 Sandip Vaghela
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

package com.afterroot.allusive2.model

import android.provider.BaseColumns
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = RoomPointer.TABLE_NAME)
data class RoomPointer(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(index = true, name = BaseColumns._ID)
    val id: Int = 0,
    @ColumnInfo val pointer_name: String?,
    @ColumnInfo val file_name: String?,
    @ColumnInfo val pointer_desc: String?,
    @ColumnInfo val uploader_id: String,
    @ColumnInfo val uploader_name: String
) {
    companion object {
        const val TABLE_NAME = "pointers"
    }
}
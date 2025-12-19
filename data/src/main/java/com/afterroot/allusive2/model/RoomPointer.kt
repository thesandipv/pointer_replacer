/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
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
  @ColumnInfo val uploader_name: String,
) {
  companion object {
    const val TABLE_NAME = "pointers"
  }
}

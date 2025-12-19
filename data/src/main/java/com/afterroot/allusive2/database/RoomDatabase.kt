/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.afterroot.allusive2.model.RoomPointer

val MIGRATION_1_2 = object : Migration(1, 2) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.apply {
      execSQL(
        "CREATE TABLE pointers_new (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, pointer_name TEXT, file_name TEXT, pointer_desc TEXT, uploader_id TEXT NOT NULL, uploader_name TEXT NOT NULL)",
      )
      execSQL("CREATE INDEX IF NOT EXISTS `index_pointers_new__id` ON pointers_new (_id)")
      execSQL(
        "INSERT INTO pointers_new (pointer_name, file_name, pointer_desc, uploader_id, uploader_name) SELECT pointer_name, file_name, pointer_desc, uploader_id, uploader_name FROM pointers",
      )
      execSQL("DROP TABLE pointers")
      execSQL("ALTER TABLE pointers_new RENAME TO pointers")
    }
  }
}

@Dao
interface PointerDao {
  @Query("SELECT * FROM pointers ORDER BY pointer_name")
  fun getAll(): LiveData<List<RoomPointer>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun add(vararg pointer: RoomPointer)

  @Delete
  suspend fun delete(pointer: RoomPointer)

  @Query("SELECT * FROM pointers WHERE file_name LIKE :fileName")
  suspend fun exists(fileName: String): List<RoomPointer>
}

@Database(entities = [RoomPointer::class], version = 2)
abstract class MyDatabase : RoomDatabase() {
  abstract fun pointerDao(): PointerDao
}

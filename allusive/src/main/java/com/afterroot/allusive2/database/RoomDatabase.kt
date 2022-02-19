/*
 * Copyright (C) 2016-2021 Sandip Vaghela
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

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.afterroot.allusive2.model.RoomPointer
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.apply {
            execSQL("CREATE TABLE pointers_new (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, pointer_name TEXT, file_name TEXT, pointer_desc TEXT, uploader_id TEXT NOT NULL, uploader_name TEXT NOT NULL)")
            execSQL("CREATE INDEX IF NOT EXISTS `index_pointers_new__id` ON pointers_new (_id)")
            execSQL("INSERT INTO pointers_new (pointer_name, file_name, pointer_desc, uploader_id, uploader_name) SELECT pointer_name, file_name, pointer_desc, uploader_id, uploader_name FROM pointers")
            execSQL("DROP TABLE pointers")
            execSQL("ALTER TABLE pointers_new RENAME TO pointers")
        }
    }
}

val roomModule = module {
    single {
        Room.databaseBuilder(
            androidApplication(),
            MyDatabase::class.java,
            "installed-pointers"
        ).addMigrations(MIGRATION_1_2).build()
    }

    single { get<MyDatabase>().pointerDao() }
}

@Dao
interface PointerDao {
    @Query("SELECT * FROM pointers ORDER BY pointer_name")
    fun getAll(): LiveData<List<RoomPointer>>

    @Insert
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

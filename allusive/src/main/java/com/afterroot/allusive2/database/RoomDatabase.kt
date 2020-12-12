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

package com.afterroot.allusive2.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.afterroot.allusive2.model.RoomPointer
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val roomModule = module {
    single {
        Room.databaseBuilder(
            androidApplication(),
            MyDatabase::class.java,
            "installed-pointers"
        ).build()
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
    //TODO Verify migration.
    abstract fun pointerDao(): PointerDao
}
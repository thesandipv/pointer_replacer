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

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.afterroot.allusive2.model.RoomPointer
import java.io.IOException
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
  private val dbName = "test_db"

  @get:Rule
  val helper: MigrationTestHelper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    MyDatabase::class.java.canonicalName,
    FrameworkSQLiteOpenHelperFactory(),
  )

  @Test
  @Throws(IOException::class)
  fun verifyMigrationQueries() {
    helper.createDatabase(dbName, 1).apply {
      // db has schema version 1. insert some data using SQL queries.
      // You cannot use DAO classes because they expect the latest schema.
      val entry = contentValuesOf().apply {
        put("_id", 1)
        put("pointer_name", "Test")
        put("file_name", "pointer34242324324.png")
        put("pointer_desc", "Test Desc")
        put("uploader_id", "45446464")
        put("uploader_name", "Test User")
      }
      insert(RoomPointer.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, entry)

      // Prepare for the next version.
      close()
    }

    // Re-open the database with version 2 and provide
    // MIGRATION_1_2 as the migration process.
    val dbv2 = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)

    // MigrationTestHelper automatically verifies the schema changes,
    // but you need to validate that the data was migrated properly.

    val query = SupportSQLiteQueryBuilder.builder(RoomPointer.TABLE_NAME).create()
    val get = dbv2.query(query)
    Assert.assertEquals(1, get.count)
  }
}

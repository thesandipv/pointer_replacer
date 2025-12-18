/*
 * Copyright 2023, Google LLC, Christopher Banes and the Tivi project contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package app.tivi.data.db

import androidx.room.withTransaction
import com.afterroot.allusive2.database.MyDatabase
import javax.inject.Inject

class RoomTransactionRunner @Inject constructor(private val db: MyDatabase) :
  DatabaseTransactionRunner {
  override suspend operator fun <T> invoke(block: suspend () -> T): T = db.withTransaction {
    block()
  }
}

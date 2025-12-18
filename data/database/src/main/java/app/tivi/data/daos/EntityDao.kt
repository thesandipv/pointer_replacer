/*
 * Copyright 2023, Google LLC, Christopher Banes and the Tivi project contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package app.tivi.data.daos

import com.afterroot.allusive2.data.model.AllusiveEntity

interface EntityDao<in E : AllusiveEntity> {
  suspend fun upsert(entity: E): Long
  suspend fun upsertAll(entities: List<E>)
  suspend fun deleteEntity(entity: E)
}

suspend inline fun <E : AllusiveEntity> EntityDao<E>.upsertAll(vararg entities: E) {
  upsertAll(entities.toList())
}

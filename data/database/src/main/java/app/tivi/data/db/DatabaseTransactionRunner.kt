/*
 * Copyright 2023, Google LLC, Christopher Banes and the Tivi project contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package app.tivi.data.db

interface DatabaseTransactionRunner {
    suspend operator fun <T> invoke(block: suspend () -> T): T
}

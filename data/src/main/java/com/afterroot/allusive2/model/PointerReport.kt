/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.model

import androidx.annotation.Keep
import com.afterroot.allusive2.Reason
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.io.Serializable
import java.util.Date

@Keep
data class PointerReport(
  var pointerId: String? = null,
  var reporterUid: String? = null,
  var reason: Int = Reason.OTHER,
  var details: String? = null,
  @ServerTimestamp var timestamp: Date = Timestamp.now().toDate(),
  @Exclude var docId: String? = null,
) : Serializable

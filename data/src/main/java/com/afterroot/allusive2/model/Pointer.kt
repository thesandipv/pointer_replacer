/*
 * Copyright (C) 2020-2025 Sandip Vaghela
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

// Collection 'pointers'
@Keep
data class Pointer(
  var name: String? = null,
  var filename: String? = null,
  var description: String? = null,
  var uploadedBy: HashMap<String, String>? = null,
  @ServerTimestamp var time: Date = Timestamp.now().toDate(),
  var downloads: Int = 0,
  var rroDownloads: Int = 0,
  /**
   * Use constants from [Reason]
   * */
  var reasonCode: Int = Reason.OK,
  var hasRRO: Boolean = false,
  var rroRequested: Boolean = false,
  @Exclude var docId: String? = null,
) : Serializable

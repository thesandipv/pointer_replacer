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

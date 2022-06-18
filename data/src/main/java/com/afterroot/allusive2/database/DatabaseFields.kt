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

object DatabaseFields {
    // table 'users'
    const val COLLECTION_USERS = "users"
    const val FIELD_NAME = "name"
    const val FIELD_EMAIL = "email"
    const val FIELD_UID = "uid"
    const val FIELD_FCM_ID = "fcmId"

    // table 'pointers'
    const val COLLECTION_POINTERS = "pointers"
    const val FIELD_DESC = "description"
    const val FIELD_FILENAME = "filename"
    const val FIELD_TIME = "time"
    const val FIELD_UPLOAD_BY = "uploadedBy"
    const val FIELD_DOWNLOADS = "downloads"
    const val FIELD_RRO_DOWNLOADS = "rroDownloads"
    const val FIELD_UPVOTES = "upvotes"
    const val FIELD_DOWNVOTES = "downvotes"
    const val FIELD_HAS_RRO = "hasRRO"
    const val FIELD_RRO_REQUESTED = "rroRequested"

    // table 'requests'
    const val COLLECTION_REQUESTS = "requests"
    const val FIELD_REQUEST_CLOSED = "isRequestClosed"
    const val FIELD_TIMESTAMP = "timestamp"
}

/*
 * Copyright (C) 2016-2022 Sandip Vaghela
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
package com.afterroot.allusive2.data

import com.afterroot.allusive2.database.DatabaseFields
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

fun FirebaseFirestore.pointers() = collection(DatabaseFields.COLLECTION_POINTERS)
fun FirebaseFirestore.requests() = collection(DatabaseFields.COLLECTION_REQUESTS)
fun FirebaseStorage.pointers() = reference.child(DatabaseFields.COLLECTION_POINTERS)

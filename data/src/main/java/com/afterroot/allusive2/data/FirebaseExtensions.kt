/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.data

import com.afterroot.allusive2.database.DatabaseFields
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

fun FirebaseFirestore.pointers() = collection(DatabaseFields.COLLECTION_POINTERS)
fun FirebaseFirestore.requests() = collection(DatabaseFields.COLLECTION_REQUESTS)
fun FirebaseStorage.pointers() = reference.child(DatabaseFields.COLLECTION_POINTERS)

/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.data.stub

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import com.afterroot.allusive2.data.pointers
import com.afterroot.allusive2.model.Pointer
import com.afterroot.data.utils.FirebaseUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

fun createStubPointers(
  firestore: FirebaseFirestore,
  storage: FirebaseStorage,
  firebaseUtils: FirebaseUtils,
) {
  repeat(50) { i ->
    val filename = "pointer$i.png"
    val pointer = Pointer(
      name = "Pointer $i",
      filename = filename,
      description = "Awesome Pointer $i",
      uploadedBy = hashMapOf(Pair(firebaseUtils.uid, "Awesome User")),
      time = Timestamp.now().toDate(),
    )
    firestore.pointers().document().set(pointer)

    val bitmap = createBitmap(128, 128)
    val canvas = Canvas(bitmap)
    val circlePaint = Paint().apply {
      color = Color.HSVToColor(floatArrayOf((i * 360f / 50f), 0.8f, 0.9f))
      isAntiAlias = true
    }
    canvas.drawCircle(64f, 64f, 48f, circlePaint)
    val textPaint = Paint().apply {
      color = Color.WHITE
      textSize = 32f
      isAntiAlias = true
      textAlign = Paint.Align.CENTER
    }
    canvas.drawText("$i", 64f, 76f, textPaint)

    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    val byteArray = stream.toByteArray()
    storage.pointers().child(filename).putBytes(byteArray)
  }
}

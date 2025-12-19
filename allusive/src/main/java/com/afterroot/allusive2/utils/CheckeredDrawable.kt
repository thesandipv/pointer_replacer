/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.utils

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable

class CheckeredDrawable(var size: Int = 20) : Drawable() {

  private val mPaint = Paint(ANTI_ALIAS_FLAG)

  override fun draw(canvas: Canvas) {
    val bitmap = Bitmap.createBitmap(size * 2, size * 2, Bitmap.Config.ARGB_8888)
    val p = Paint(ANTI_ALIAS_FLAG)
    p.style = Paint.Style.FILL

    val c = Canvas(bitmap)

    val r = Rect(0, 0, size, size)
    p.color = Color.DKGRAY
    c.drawRect(r, p)

    r.offset(size, size)
    c.drawRect(r, p)

    p.color = Color.LTGRAY
    r.offset(-size, 0)
    c.drawRect(r, p)

    r.offset(size, -size)
    c.drawRect(r, p)

    mPaint.shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)

    canvas.drawPaint(mPaint)
  }

  override fun setAlpha(alpha: Int) {
    mPaint.alpha = alpha
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    mPaint.colorFilter = colorFilter
  }

  override fun getOpacity(): Int = PixelFormat.TRANSPARENT
}

/*
 * Copyright (C) 2016-2020 Sandip Vaghela
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

package com.afterroot.allusive2

import android.content.Context

object Constants {
    const val POINTER_MOUSE = 2
    const val POINTER_TOUCH = 1
    const val PREF_KEY_FIRST_INSTALL = "first_install_2"
    const val RC_LOGIN = 42
    const val RC_PERMISSION = 256
    const val RC_PICK_IMAGE: Int = 478
}

object Reason {
    const val DUPLICATE = 1
    const val NOT_A_POINTER = 2
    const val OTHER = 3
}

fun Context.getMinPointerSize(): Int = this.resources.getInteger(R.integer.min_pointer_size)

fun Context.getMinPointerSizePx(): Int = (this.getMinPointerSize() * this.resources.displayMetrics.density.toInt()) / 160
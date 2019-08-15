/*
 * Copyright (C) 2016-2019 Sandip Vaghela
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

package com.afterroot.allusive.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.afterroot.allusive.GlideApp
import com.afterroot.allusive.R
import com.afterroot.allusive.adapter.callback.ItemSelectedCallback
import com.afterroot.allusive.model.IPointer
import com.afterroot.allusive.model.Pointer
import com.afterroot.allusive.utils.CheckeredDrawable
import com.afterroot.allusive.utils.getMinPointerSize
import com.afterroot.allusive.utils.inflate
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.item_pointer_repo.view.*

class PointerDelegateAdapter(val callbacks: ItemSelectedCallback) : TypeDelegateAdapter {
    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder = PointerVH(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: IPointer) {
        holder as PointerVH
        holder.bind(item as Pointer)
    }

    inner class PointerVH(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.item_pointer_repo)) {
        val context: Context = parent.context
        private val itemName: AppCompatTextView = itemView.info_pointer_pack_name
        private val itemThumb: AppCompatImageView = itemView.info_pointer_image
        private val itemUploader: AppCompatTextView = itemView.info_username


        fun bind(pointer: Pointer) {
            val storageReference = FirebaseStorage.getInstance().reference.child("pointers/${pointer.filename}")
            itemName.text = pointer.name
            pointer.uploadedBy!!.forEach {
                itemUploader.text = String.format(context.getString(R.string.str_format_uploaded_by), it.value)
            }
            GlideApp.with(context).load(storageReference).override(context.getMinPointerSize(), context.getMinPointerSize())
                .into(itemThumb)
            itemThumb.background =
                CheckeredDrawable().apply { alpha = context.resources.getInteger(R.integer.checkered_grid_alpha) }
            with(super.itemView) {
                tag = pointer
                setOnClickListener {
                    callbacks.onClick(adapterPosition, itemView)
                }
                setOnLongClickListener {
                    callbacks.onLongClick(adapterPosition)
                    return@setOnLongClickListener true
                }
            }
        }
    }
}


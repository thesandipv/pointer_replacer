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
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.afterroot.allusive.GlideApp
import com.afterroot.allusive.R
import com.afterroot.allusive.adapter.callback.ItemSelectedCallback
import com.afterroot.allusive.model.IPointer
import com.afterroot.allusive.model.Pointer
import com.afterroot.allusive.utils.getMinPointerSize
import com.afterroot.allusive.utils.inflate
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.repo_pointer_item.view.*

class PointerDelegateAdapter(val callbacks: ItemSelectedCallback) : TypeDelegateAdapter {
    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder = PointerVH(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: IPointer) {
        holder as PointerVH
        holder.bind(item as Pointer)
    }

    inner class PointerVH(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.repo_pointer_item)) {
        val context: Context = parent.context
        private val itemName: AppCompatTextView = itemView.item_pointer_pack_name
        private val itemDesc: AppCompatTextView = itemView.item_pack_desc
        private val itemThumb: AppCompatImageView = itemView.item_pointer_thumb
        private val actionDl: AppCompatImageButton = itemView.item_action_pack
        private val itemUploader: AppCompatTextView = itemView.item_username


        fun bind(pointer: Pointer) {
            val storageReference = FirebaseStorage.getInstance().reference.child("pointers/${pointer.filename}")
            itemName.text = pointer.name
            itemDesc.text = pointer.description
            pointer.uploadedBy!!.forEach {
                itemUploader.text = String.format(context.getString(R.string.formar_uploaded_by), it.value)
            }
            GlideApp.with(context).load(storageReference).override(context.getMinPointerSize()).into(itemThumb)
            actionDl.setOnClickListener {
                callbacks.onClick(adapterPosition, it)
            }

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


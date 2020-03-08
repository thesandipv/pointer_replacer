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

package com.afterroot.allusive.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.afterroot.allusive.GlideApp
import com.afterroot.allusive.R
import com.afterroot.allusive.adapter.callback.ItemSelectedCallback
import com.afterroot.allusive.getMinPointerSize
import com.afterroot.allusive.model.Pointer
import com.afterroot.core.extensions.getDrawableExt
import com.afterroot.core.extensions.inflate
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.item_pointer_repo.view.*

class MyFirestorePagingAdapter(options: FirestorePagingOptions<Pointer>, val callbacks: ItemSelectedCallback) :
    FirestorePagingAdapter<Pointer, MyFirestorePagingAdapter.PointerVH>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PointerVH = PointerVH(parent)

    override fun onBindViewHolder(viewholder: PointerVH, type: Int, pointer: Pointer) =
        viewholder.bind(pointer)

    inner class PointerVH(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.item_pointer_repo)) {
        val context: Context = parent.context

        fun bind(pointer: Pointer) {
            val storageReference = FirebaseStorage.getInstance().reference.child("pointers/${pointer.filename}")
            itemView.info_pointer_pack_name.text = pointer.name
            pointer.uploadedBy!!.forEach {
                itemView.info_username.text = String.format(context.getString(R.string.str_format_uploaded_by), it.value)
            }
            itemView.info_pointer_image.apply {
                if (pointer.reasonCode <= 0) {
                    GlideApp.with(context).load(storageReference)
                        .override(context.getMinPointerSize(), context.getMinPointerSize())
                        .into(this)
                    background = context.getDrawableExt(R.drawable.transparent_grid)
                } else {
                    background = null
                    setImageDrawable(context.getDrawableExt(R.drawable.ic_removed, R.color.color_error))
                }

            }

            itemView.info_meta.text = String.format(context.getString(R.string.str_format_download_count), pointer.downloads)

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
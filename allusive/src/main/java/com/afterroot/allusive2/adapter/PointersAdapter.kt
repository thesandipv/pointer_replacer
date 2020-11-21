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

package com.afterroot.allusive2.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.afterroot.allusive2.GlideApp
import com.afterroot.allusive2.R
import com.afterroot.allusive2.Reason
import com.afterroot.allusive2.adapter.callback.ItemSelectedCallback
import com.afterroot.allusive2.getMinPointerSize
import com.afterroot.allusive2.model.Pointer
import com.afterroot.core.extensions.getDrawableExt
import com.afterroot.core.extensions.inflate
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.item_pointer_repo.view.*
import org.koin.core.KoinComponent
import org.koin.core.get


/**
 * New list adapter for Repository screen.
 * */
class PointersAdapter(private val callbacks: ItemSelectedCallback<Pointer>) :
    ListAdapter<Pointer, RecyclerView.ViewHolder>(object : DiffUtil.ItemCallback<Pointer?>() {
        override fun areItemsTheSame(oldItem: Pointer, newItem: Pointer): Boolean = oldItem == newItem

        override fun areContentsTheSame(oldItem: Pointer, newItem: Pointer): Boolean =
            oldItem.hashCode() == newItem.hashCode()
    }) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return PointerVH(parent, callbacks)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as PointerVH
        holder.bind(getItem(position))
    }

    inner class PointerVH(parent: ViewGroup, private val callbacks: ItemSelectedCallback<Pointer>) :
        RecyclerView.ViewHolder(parent.inflate(R.layout.item_pointer_repo)), KoinComponent {
        val context: Context = parent.context
        private val itemName: AppCompatTextView = itemView.info_pointer_pack_name
        private val itemThumb: AppCompatImageView = itemView.info_pointer_image
        private val itemUploader: AppCompatTextView = itemView.info_username


        fun bind(pointer: Pointer) {
            when (pointer.reasonCode) {
                Reason.OK -> {
                    val storageReference = get<FirebaseStorage>().reference.child("pointers/${pointer.filename}")
                    itemName.text = pointer.name
                    pointer.uploadedBy?.forEach {
                        itemUploader.text = String.format(context.getString(R.string.str_format_uploaded_by), it.value)
                    }
                    itemThumb.apply {
                        updateLayoutParams<ConstraintLayout.LayoutParams> {
                            height = context.getMinPointerSize()
                            width = context.getMinPointerSize()
                        }
                        val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
                        GlideApp.with(context)
                            .load(storageReference)
                            .override(context.getMinPointerSize(), context.getMinPointerSize())
                            .transition(DrawableTransitionOptions.withCrossFade(factory))
                            .into(this)
                        background = context.getDrawableExt(R.drawable.transparent_grid)

                    }
                    itemView.info_meta.text =
                        context.resources.getQuantityString(
                            R.plurals.str_format_download_count,
                            pointer.downloads,
                            pointer.downloads
                        )
                }
                else -> {
                    itemThumb.apply {
                        updateLayoutParams<ConstraintLayout.LayoutParams> {
                            height = context.getMinPointerSize()
                            width = context.getMinPointerSize()
                        }
                        background = null
                        setImageDrawable(context.getDrawableExt(R.drawable.ic_removed, R.color.color_error))
                    }
                    itemName.text = pointer.name
                    itemView.info_meta.text = null
                    itemUploader.text = null
                }
            }

            with(super.itemView) {
                tag = pointer
                setOnClickListener {
                    callbacks.onClick(adapterPosition, itemView, pointer)
                }
                setOnLongClickListener {
                    callbacks.onLongClick(adapterPosition, pointer)
                    return@setOnLongClickListener true
                }
            }
        }
    }
}
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
package com.afterroot.allusive2.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.afterroot.allusive2.GlideApp
import com.afterroot.allusive2.adapter.callback.ItemSelectedCallback
import com.afterroot.allusive2.getMinPointerSize
import com.afterroot.allusive2.getPointerSaveDir
import com.afterroot.allusive2.model.RoomPointer
import com.afterroot.allusive2.repo.databinding.ItemPointerRepoBinding
import com.afterroot.core.extensions.getDrawableExt
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.afterroot.allusive2.resources.R as CommonR

/**
 * New list adapter for Pointer Choose screen.
 * */
class LocalPointersAdapter(private val callbacks: ItemSelectedCallback<RoomPointer>) :
    ListAdapter<RoomPointer, RecyclerView.ViewHolder>(object : DiffUtil.ItemCallback<RoomPointer?>() {
        override fun areItemsTheSame(oldItem: RoomPointer, newItem: RoomPointer): Boolean = oldItem == newItem

        override fun areContentsTheSame(oldItem: RoomPointer, newItem: RoomPointer): Boolean =
            oldItem.hashCode() == newItem.hashCode()
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemPointerRepoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PointerVH(binding, callbacks)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as PointerVH
        holder.bind(getItem(position))
    }

    inner class PointerVH(binding: ItemPointerRepoBinding, private val callbacks: ItemSelectedCallback<RoomPointer>) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context
        private val itemName: AppCompatTextView = binding.infoPointerPackName
        private val itemThumb: AppCompatImageView = binding.infoPointerImage
        private val itemUploader: AppCompatTextView = binding.infoUsername

        fun bind(pointer: RoomPointer) {
            itemName.text = pointer.pointer_name
            itemUploader.text =
                String.format(context.getString(CommonR.string.str_format_uploaded_by), pointer.uploader_name)
            itemThumb.apply {
                updateLayoutParams<ConstraintLayout.LayoutParams> {
                    height = context.getMinPointerSize()
                    width = context.getMinPointerSize()
                }
                val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
                GlideApp.with(context)
                    .load("${context.getPointerSaveDir()}${pointer.file_name}")
                    .override(context.getMinPointerSize(), context.getMinPointerSize())
                    .transition(DrawableTransitionOptions.withCrossFade(factory))
                    .into(this)
                background = context.getDrawableExt(CommonR.drawable.transparent_grid)
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

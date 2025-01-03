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
package com.afterroot.allusive2.repo

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.afterroot.allusive2.Reason
import com.afterroot.allusive2.adapter.callback.ItemSelectedCallback
import com.afterroot.allusive2.getMinPointerSize
import com.afterroot.allusive2.model.Pointer
import com.afterroot.allusive2.repo.databinding.ItemPointerRepoBinding
import com.afterroot.utils.extensions.getDrawableExt
import com.afterroot.utils.extensions.getTintedDrawable
import com.afterroot.utils.extensions.visible
import com.afterroot.utils.getMaterialColor
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.firebase.storage.FirebaseStorage
import com.afterroot.allusive2.resources.R as CommonR

class PointerPagingAdapter(
  private val callbacks: ItemSelectedCallback<Pointer>,
  private val firebaseStorage: FirebaseStorage,
) : PagingDataAdapter<Pointer, RecyclerView.ViewHolder>(Companion) {

  companion object : DiffUtil.ItemCallback<Pointer>() {
    override fun areItemsTheSame(oldItem: Pointer, newItem: Pointer): Boolean =
      oldItem.filename == newItem.filename
    override fun areContentsTheSame(oldItem: Pointer, newItem: Pointer): Boolean =
      oldItem == newItem
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val binding = ItemPointerRepoBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false,
    )
    return PointerVH(binding, callbacks, firebaseStorage)
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    holder as PointerVH
    getItem(position)?.let { holder.bind(it) }
  }
}

class PointerVH(
  binding: ItemPointerRepoBinding,
  private val callbacks: ItemSelectedCallback<Pointer>,
  private val storage: FirebaseStorage,
) : RecyclerView.ViewHolder(binding.root) {
  val context: Context = binding.root.context
  private val itemName: AppCompatTextView = binding.infoPointerPackName
  private val itemThumb: AppCompatImageView = binding.infoPointerImage
  private val itemUploader: AppCompatTextView = binding.infoUsername
  private val infoMeta: AppCompatTextView = binding.infoMeta
  private val metaRRO: AppCompatTextView = binding.metaRro

  fun bind(pointer: Pointer) {
    when (pointer.reasonCode) {
      Reason.OK -> {
        val storageReference = storage.reference.child("pointers/${pointer.filename}")
        itemName.text = pointer.name
        pointer.uploadedBy?.forEach {
          itemUploader.text =
            String.format(
              context.getString(CommonR.string.str_format_uploaded_by),
              it.value,
            )
        }
        itemThumb.apply {
          updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = context.getMinPointerSize()
            width = context.getMinPointerSize()
          }
          val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(
            true,
          ).build()
          Glide.with(context)
            .load(storageReference)
            .override(context.getMinPointerSize(), context.getMinPointerSize())
            .transition(DrawableTransitionOptions.withCrossFade(factory))
            .into(this)
          background = context.getDrawableExt(CommonR.drawable.transparent_grid)
        }
        infoMeta.text = pointer.downloads.toString()
        metaRRO.apply {
          visible(pointer.hasRRO)
          if (pointer.hasRRO) {
            text = pointer.rroDownloads.toString()
          }
        }
      }
      else -> {
        itemThumb.apply {
          updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = context.getMinPointerSize()
            width = context.getMinPointerSize()
          }
          background = null
          setImageDrawable(
            context.getTintedDrawable(
              CommonR.drawable.ic_removed,
              getMaterialColor(com.google.android.material.R.attr.colorError),
            ),
          )
        }
        itemName.text = pointer.name
        infoMeta.text = null
        itemUploader.text = null
      }
    }

    with(super.itemView) {
      tag = pointer
      setOnClickListener {
        callbacks.onClick(absoluteAdapterPosition, itemView, pointer)
      }
      setOnLongClickListener {
        return@setOnLongClickListener callbacks.onLongClick(
          absoluteAdapterPosition,
          pointer,
        )
      }
    }
  }
}

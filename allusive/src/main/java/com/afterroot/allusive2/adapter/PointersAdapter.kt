/*
 * Copyright (C) 2020-2025 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.afterroot.allusive2.adapter.callback.ItemSelectedCallback
import com.afterroot.allusive2.model.Pointer
import com.afterroot.allusive2.repo.PointerVH
import com.afterroot.allusive2.repo.databinding.ItemPointerRepoBinding
import com.google.firebase.storage.FirebaseStorage

/**
 * New list adapter for Repository screen.
 * */
class PointersAdapter(
  private val callbacks: ItemSelectedCallback<Pointer>,
  private val firebaseStorage: FirebaseStorage,
) : ListAdapter<Pointer, RecyclerView.ViewHolder>(object : DiffUtil.ItemCallback<Pointer?>() {
  override fun areItemsTheSame(oldItem: Pointer, newItem: Pointer): Boolean = oldItem == newItem

  override fun areContentsTheSame(oldItem: Pointer, newItem: Pointer): Boolean =
    oldItem.hashCode() == newItem.hashCode()
}) {
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
    holder.bind(getItem(position))
  }
}

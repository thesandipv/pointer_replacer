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
class PointersAdapter(private val callbacks: ItemSelectedCallback<Pointer>, private val firebaseStorage: FirebaseStorage) :
    ListAdapter<Pointer, RecyclerView.ViewHolder>(object : DiffUtil.ItemCallback<Pointer?>() {
        override fun areItemsTheSame(oldItem: Pointer, newItem: Pointer): Boolean = oldItem == newItem

        override fun areContentsTheSame(oldItem: Pointer, newItem: Pointer): Boolean =
            oldItem.hashCode() == newItem.hashCode()
    }) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemPointerRepoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PointerVH(binding, callbacks, firebaseStorage)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as PointerVH
        holder.bind(getItem(position))
    }
}

/*
 * Copyright (C) 2016-2018 Sandip Vaghela
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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import com.afterroot.allusive.R
import java.util.*

/**GridView Image Adapter. */
class PointerAdapter(private val mContext: Context) : BaseAdapter() {
    private val inflater: LayoutInflater = LayoutInflater.from(mContext)

    companion object {
        var itemList = ArrayList<String>()
    }

    var gridImageWidth = 49

    fun clear() {
        itemList.clear()
    }

    fun getPath(index: Int): String {
        return itemList[index]
    }

    override fun getCount(): Int {
        return itemList.size
    }

    override fun getItem(arg0: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    fun setLayoutParams(i: Int) {
        gridImageWidth = i
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val holder: ViewHolder
        var view: View? = convertView

        if (view == null) {
            view = inflater.inflate(R.layout.gridview_item, parent, false)
            holder = ViewHolder()
            holder.imageView = view.findViewById(R.id.grid_item_image)
            holder.imageView!!.layoutParams = FrameLayout.LayoutParams(gridImageWidth, gridImageWidth)
            view.tag = holder
        } else {
            holder = view.tag as ViewHolder
        }

        //Glide.with(mContext).load(itemList[position]).into(holder.imageView!!)

        return view
    }
}

private class ViewHolder {
    internal var imageView: ImageView? = null
}
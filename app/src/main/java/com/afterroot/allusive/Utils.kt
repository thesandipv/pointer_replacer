/*
 * Copyright (C) 2016-2017 Sandip Vaghela
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

package com.afterroot.allusive

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.support.design.widget.Snackbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.*
import com.bumptech.glide.Glide
import java.io.File
import java.util.*

/**
 * Helper Class
 */
internal class Utils {

    fun isAppInstalled(context: Context, pName: String): Boolean {
        try {
            context.packageManager.getApplicationInfo(pName, 0)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }

    }

    fun showSnackBar(view: View, message: String) {
        this.showSnackBar(view, message, null)
    }

    fun showSnackBar(view: View, message: String, action: String?) {
        this.showSnackBar(view, message, action, android.view.View.OnClickListener {  })
    }

    fun showSnackBar(view: View, message: String, action: String?, action_listener: View.OnClickListener) {
        this.showSnackBar(view, message, Snackbar.LENGTH_LONG, action, action_listener)
    }

    fun showSnackBar(view: View, message: String, length: Int, action: String?, action_listener: View.OnClickListener) {
        val snackBar = Snackbar.make(view, message, length)
        snackBar.setAction(action, action_listener)
        snackBar.show()
    }

    fun getDpi(context: Context): Int {
        return context.resources.displayMetrics.densityDpi
    }

    /**
     * @gridImageWidth fileName name of file
     * @return extension of fileName
     */
    private fun getFileExt(fileName: String): String {
        return fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length)
    }

    /**
     * @gridImageWidth fileName name of file
     * @return mime type of fileName
     */
    private fun getMimeType(fileName: String): String? {
        var type: String? = null
        try {
            val extension = getFileExt(fileName)
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return type
    }

    fun loadToBottomSheetGrid(context: Context,
                              target: GridView,
                              targetPath: String,
                              listener: AdapterView.OnItemClickListener) {
        val pointerAdapter = PointerAdapter(context)
        if (getDpi(context) <= 240) {
            pointerAdapter.setLayoutParams(49)
        } else if (getDpi(context) >= 240) {
            pointerAdapter.setLayoutParams(66)
        }

        target.adapter = pointerAdapter
        target.onItemClickListener = listener

        val files = File(targetPath).listFiles()
        try {
            for (file in files) {
                pointerAdapter.add(file.absolutePath)
            }
        } catch (npe: NullPointerException) {
            npe.printStackTrace()
        }

    }

    /**GridView Image Adapter. */
    internal class PointerAdapter(private val mContext: Context) : BaseAdapter() {
        private val inflater: LayoutInflater = LayoutInflater.from(mContext)

        companion object {
            var itemList = ArrayList<String>()
        }

        var gridImageWidth = 49

        fun add(path: String) {
            itemList.add(path)
        }

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

        override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
            val holder: ViewHolder
            var view: View? = convertView

            if (view == null) {
                view = inflater.inflate(R.layout.gridview_item, parent, false)
                holder = ViewHolder()
                holder.imageView = view!!.findViewById(R.id.grid_item_image)
                holder.imageView!!.layoutParams = FrameLayout.LayoutParams(gridImageWidth, gridImageWidth)
                view.tag = holder
            } else {
                holder = view.tag as ViewHolder
            }

            Glide.with(mContext).load(itemList[position]).into(holder.imageView!!)

            return view
        }
    }

    private class ViewHolder {
        internal var imageView: ImageView? = null
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    fun openFile(context: Context, filename: String, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, getMimeType(filename))
        context.startActivity(intent)
    }
}

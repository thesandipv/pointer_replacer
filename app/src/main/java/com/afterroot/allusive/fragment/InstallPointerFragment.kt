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

package com.afterroot.allusive.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.widget.AppCompatTextView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.allusive.Helper
import com.afterroot.allusive.R
import com.afterroot.allusive.adapter.PointerAdapter
import kotlinx.android.synthetic.main.content_manage_pointer.*
import kotlinx.android.synthetic.main.fragment_install_pointer.*
import kotlinx.android.synthetic.main.manage_pointer_list_item.view.*
import java.io.*
import java.util.*

class InstallPointerFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_install_pointer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

    }

    @SuppressLint("CommitPrefEdits")
    override fun onStart() {
        super.onStart()
        val pointersFolder = getString(R.string.pointer_folder_path)
        val extSdDir = Environment.getExternalStorageDirectory().toString()
        mTargetPath = extSdDir + pointersFolder
        mTag = "ManagePointers"
        mUtils = Helper

        mInstalledPointerPrefs = activity!!.getSharedPreferences(activity!!.packageName + ".installed_pointers", Context.MODE_PRIVATE)
        mInstalledPointerEditor = Companion.mInstalledPointerPrefs!!.edit()

        val pointerPackList: RecyclerView = activity!!.pointerPackList

        target = File(mTargetPath)
        targetFiles = target!!.listFiles()

        val list = ArrayList<String>()
        list.add(0, DEFAULT_PP_NAME)
        list.add(1, CHRISTMAS_PP_NAME)
        list.add(2, HEART_PP_NAME)
        list.add(3, GOOGLE_MI_PP_NAME)
        list.add(4, HERMANKZR_PP_NAME)
        list.add(5, XDA_POINTER_NAME)
        list.add(6, POKEMON_PP_NAME)


        val adapter = PointerListAdapter(activity!!, list)

        pointerPackList.adapter = adapter
        pointerPackList.layoutManager = LinearLayoutManager(activity!!)

    }

    class PointerListAdapter(context: Activity, list: ArrayList<String>) : RecyclerView.Adapter<PointerPackViewHolder>() {
        private val TAG = "PointerListAdapter"
        val pointerList = list
        private val mContext = context
        private var installedState: AppCompatTextView? = null
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PointerPackViewHolder {
            return PointerPackViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.manage_pointer_list_item, parent, false))
        }

        override fun getItemCount(): Int {
            return pointerList.size
        }

        override fun onBindViewHolder(holder: PointerPackViewHolder, position: Int) {
            holder.packName.text = pointerList[position]
        }

        private fun copySpecificPointer(nameStartsWith: String) {
            val am = mContext.assets
            var files: Array<String>? = null

            try {
                files = am.list(DIR_NAME_POINTERS)
            } catch (e: IOException) {
                Log.e(TAG, "copySpecificPointer: ${e.message}")
            }

            assert(files != null)
            for (filename in files!!) {
                if (filename.startsWith(nameStartsWith)) {
                    val inputStream: InputStream
                    val out: OutputStream
                    try {
                        inputStream = am.open(DIR_NAME_POINTERS + File.separator + filename)
                        out = FileOutputStream(mTargetPath + filename)

                        val bytes = ByteArray(1024)
                        val len = inputStream.read(bytes)
                        while (len != -1) {
                            out.write(bytes, 0, len)
                        }

                        inputStream.close()
                        out.flush()
                        out.close()
                        notifyDataSetChanged()
                    } catch (e: Exception) {
                        Log.e(TAG, "copySpecificPointer: ${e.message}")
                    }

                }
            }
        }

        @Throws(IOException::class)
        private fun copyFile(source: File, destination: File) {
            val sourceChannel = FileInputStream(source).channel
            val destinationChannel = FileOutputStream(destination).channel
            try {
                sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel)
            } finally {
                try {
                    sourceChannel.close()
                    destinationChannel.close()
                } catch (io: IOException) {
                    io.printStackTrace()
                }

            }
        }

        internal fun deletePack(packName: String) {
            targetFiles!!.filter { it.name.startsWith(packName) }.forEach { it.delete() }
            mInstalledPointerEditor!!.putBoolean(packName, false).apply()
        }

        internal fun installPack(packName: String) {
            copySpecificPointer(packName)
            if (installedState != null) {
                installedState!!.setText(R.string.text_installed)
            }
            mInstalledPointerEditor!!.putBoolean(packName, true).apply()
            mUtils!!.showSnackBar(mContext.content_pointer_manage_root, "Pointer Pack Installed.")
        }

        internal fun showSpecificPointerDialog(pointerPackName: String, dialogTitle: String) {
            val am = mContext.assets
            var files: Array<String>? = null
            val arrayList = java.util.ArrayList<File>()

            try {
                files = am.list(DIR_NAME_POINTERS)
            } catch (e: IOException) {
                Log.e(TAG, "showSpecificPointerDialog: ${e.message}")
            }

            val pointerAdapter = PointerAdapter(mContext)
            assert(files != null)
            for (filename in files!!) {
                if (filename.startsWith(pointerPackName)) {
                    val inputStream: InputStream
                    val out: OutputStream
                    try {
                        inputStream = am.open("$DIR_NAME_POINTERS/$filename")
                        out = FileOutputStream("${mContext.cacheDir}/$filename")
                        arrayList.add(File("${mContext.cacheDir}/$filename"))

                        val bytes = ByteArray(1024)
                        val read = inputStream.read(bytes)
                        while (read != -1) {
                            out.write(bytes, 0, read)
                        }

                        inputStream.close()
                        out.flush()
                        out.close()
                        notifyDataSetChanged()
                    } catch (e: Exception) {
                        Log.e(TAG, "showSpecificPointerDialog: ${e.message}")
                    }

                }
            }
            arrayList.filter { it.name.startsWith(pointerPackName) }
                    .forEach { PointerAdapter.itemList.add(it.path) }
            val materialDialog = MaterialDialog.Builder(mContext)
                    .title(dialogTitle)
                    .customView(R.layout.layout_grid_bottomsheet, false)
                    .show()
            materialDialog.setOnDismissListener { dialogInterface -> pointerAdapter.clear() }
            val view1 = materialDialog.customView
            if (view1 != null) {
                val gridView = view1.findViewById<GridView>(R.id.grid_pointers)
                gridView.adapter = pointerAdapter
                gridView.setOnItemClickListener { adapterView, view, i, l ->
                    val source = File(pointerAdapter.getPath(i))
                    val targetFile = File(mTargetPath + source.name)
                    if (!targetFile.exists()) {
                        try {
                            copyFile(source, targetFile)
                            Toast.makeText(mContext,
                                    String.format("%s %s", source.name, mContext.getString(R.string.text_installed)),
                                    Toast.LENGTH_SHORT).show()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                    } else {
                        Toast.makeText(mContext, source.name + " is already installed.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        internal fun showPokemonDownloadDialog() {

            MaterialDialog.Builder(mContext)
                    .title("Download Pokemon Pointers Pack")
                    .content("Pokemon Pointers is not installed. Do you want to download it now?")
                    .positiveText("Install")
                    .negativeText("No")
                    .onPositive { _, _ ->
                        try {
                            startActivity(mContext,
                                    Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$POKEMON_POINTERS_PACKAGE_NAME")),
                                    null)
                        } catch (exception: ActivityNotFoundException) {
                            startActivity(mContext,
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + POKEMON_POINTERS_PACKAGE_NAME)),
                                    null)
                        }
                    }
                    .show()
        }
    }

    class PointerPackViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {
        val buttonDelete = itemView!!.button_list_delete
        val buttonInstall = itemView!!.button_list_install
        val packName = itemView!!.item_pointer_pack_name
        val packState = itemView!!.text_installed_state
    }

    companion object {
        fun newInstance(): InstallPointerFragment {
            val fragment = InstallPointerFragment()
            val args = Bundle()
            //args
            fragment.arguments = args
            return fragment
        }

        val DIR_NAME_POINTERS = "pointers"
        private var mTargetPath: String? = null
        private var targetFiles: Array<File>? = null
        private var target: File? = null
        private var mInstalledPointerEditor: SharedPreferences.Editor? = null
        private var mInstalledPointerPrefs: SharedPreferences? = null

        private val DEFAULT_POINTER_PACK = "pointer"
        private val CHRISTMAS_POINTER_PACK = "Christmas"
        private val HEART_POINTER_PACK = "emoji"
        private val GOOGLE_MI_PACK = "GoogleMaterialIcons"
        private val HERMANKZR_PACK = "Hermankzr"
        private val XDA_POINTER = "xda"
        private val POKEMON_POINTER_PACK = "Pokemon"

        private val DEFAULT_PP_NAME = "Default Pointers"
        private val CHRISTMAS_PP_NAME = "Christmas Pointers"
        private val HEART_PP_NAME = "Heart Pointers"
        private val GOOGLE_MI_PP_NAME = "GoogleMaterialIcons"
        private val HERMANKZR_PP_NAME = "User Submitted: Hermankzr"
        private val XDA_POINTER_NAME = "XDA Pointer"
        private val POKEMON_PP_NAME = "Pokemon Pointers"

        private var POKEMON_POINTERS_PACKAGE_NAME: String? = null
        private var mUtils: Helper? = null
        private var mTag: String? = null
    }
}
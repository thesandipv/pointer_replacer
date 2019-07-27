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

package com.afterroot.allusive.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.navigation.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.allusive.R
import com.afterroot.allusive.adapter.PointerAdapter
import com.afterroot.allusive.utils.visible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.layout_grid_bottomsheet.view.*
import java.io.File


class PointerBottomSheetFragment : BottomSheetDialogFragment() {
    private var mBehavior: BottomSheetBehavior<*>? = null
    private var mTitle: String? = null

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val contentView = View.inflate(context, R.layout.layout_grid_bottomsheet, container)
        dialog.setContentView(contentView)

        val params = (contentView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
        mBehavior = params.behavior as BottomSheetBehavior<*>?

        if (mBehavior != null && mBehavior is BottomSheetBehavior<*>) {
            mBehavior!!.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                @SuppressLint("SwitchIntDef")
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            dialog.dismiss()
                        }
                        BottomSheetBehavior.STATE_COLLAPSED -> {

                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    //arrow!!.progress = if (slideOffset >= 0) slideOffset else 1f
                }

            })
            mBehavior!!.peekHeight = 300
            mBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED
        }

        val pointerAdapter = PointerAdapter(activity!!)

        contentView.grid_pointers.apply {
            adapter = pointerAdapter
            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                mPointerSelectCallback!!.onPointerSelected(pointerAdapter.getItem(position))
                dialog.dismiss()
            }
        }

        try {
            val pointersFolder =
                File(Environment.getExternalStorageDirectory().toString() + getString(R.string.pointer_folder_path))
            if (!pointersFolder.exists()) pointersFolder.mkdirs()
            val pointerFiles = pointersFolder.listFiles()
            Log.d("_tag", "Total Pointers: ${pointerFiles.size}")
            PointerAdapter.itemList.clear()
            if (pointerFiles.isNotEmpty()) {
                contentView.info_no_pointer_installed.visibility = View.GONE
                contentView.text_bottomsheet_header.text = mTitle
                pointerFiles.mapTo(PointerAdapter.itemList) { it.absolutePath }

                contentView.grid_pointers.onItemLongClickListener =
                    AdapterView.OnItemLongClickListener { _, _, i, _ ->
                        val file = File(pointerAdapter.getItem(i))
                        MaterialDialog(activity!!).show {
                            title(text = getString(R.string.text_delete) + file.name)
                            message(res = R.string.text_delete_confirm)
                            positiveButton(res = R.string.text_yes) {
                                if (file.delete()) {
                                    Toast.makeText(context, "Pointer deleted.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error deleting pointer.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            negativeButton(res = R.string.text_no)
                        }
                        false
                    }
            } else {
                contentView.apply {
                    info_no_pointer_installed.visible(true)
                    bs_button_install_pointers.setOnClickListener {
                        dialog.dismiss()
                        activity!!.findNavController(R.id.fragment_repo_nav)
                            .navigate(R.id.repoFragment)
                    }
                }
            }
        } catch (npe: NullPointerException) {
            npe.printStackTrace()
        }
    }

    fun setTitle(title: String) {
        mTitle = title
    }

    private var mPointerSelectCallback: PointerSelectCallback? = null
    fun setPointerSelectCallback(callback: PointerSelectCallback) {
        mPointerSelectCallback = callback
    }

    interface PointerSelectCallback {
        fun onPointerSelected(pointerPath: String)
    }
}
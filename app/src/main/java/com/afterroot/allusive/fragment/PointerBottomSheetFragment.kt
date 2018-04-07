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
import android.app.Dialog
import android.os.Environment
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialogFragment
import android.support.design.widget.CoordinatorLayout
import android.support.v7.graphics.drawable.DrawerArrowDrawable
import android.util.Log
import android.view.View
import android.widget.AdapterView
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.allusive.Helper
import com.afterroot.allusive.MainActivity
import com.afterroot.allusive.R
import com.afterroot.allusive.adapter.PointerAdapter
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.layout_grid_bottomsheet.view.*
import java.io.File


class PointerBottomSheetFragment : BottomSheetDialogFragment() {

    var arrow: DrawerArrowDrawable? = null
    private var mBehavior: BottomSheetBehavior<*>? = null

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog?, style: Int) {
        super.setupDialog(dialog, style)
        val contentView = View.inflate(context, R.layout.layout_grid_bottomsheet, fragment_container)
        dialog!!.setContentView(contentView)

        arrow = MainActivity.toggle!!.drawerArrowDrawable

        val params = (contentView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
        mBehavior = params.behavior as BottomSheetBehavior<*>?

        if (mBehavior != null && mBehavior is BottomSheetBehavior<*>) {
            mBehavior!!.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            dialog.dismiss()
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    arrow!!.progress = if (slideOffset >= 0) slideOffset else 1f
                }

            })
            mBehavior!!.peekHeight = 300
            mBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED
        }

        val pointerAdapter = PointerAdapter(activity!!)

        if (Helper.getDpi(context!!) <= 240) {
            pointerAdapter.setLayoutParams(49)
        } else if (Helper.getDpi(context!!) >= 240) {
            pointerAdapter.setLayoutParams(66)
        }

        contentView.grid_pointers.apply {
            adapter = pointerAdapter
            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                mPointerSelectCallback!!.onPointerSelected(pointerAdapter.getPath(position))
                dialog.dismiss()
            }
        }

        try {
            val pointersFolder = File(Environment.getExternalStorageDirectory().toString() + getString(R.string.pointer_folder_path))
            if (!pointersFolder.exists()) pointersFolder.mkdirs()
            val pointerFiles = pointersFolder.listFiles()
            Log.d("TAG", "Total Pointers: ${pointerFiles.size}")
            PointerAdapter.itemList.clear()
            if (pointerFiles.isNotEmpty()) {
                contentView.info_no_pointer_installed.visibility = View.GONE
                contentView.text_bottomsheet_header.text = mTitle
                pointerFiles.mapTo(PointerAdapter.itemList) { it.absolutePath }

                contentView.grid_pointers.setOnItemLongClickListener { _, _, i, _ ->
                    val file = File(pointerAdapter.getPath(i))
                    MaterialDialog.Builder(activity!!)
                            .title(getString(R.string.text_delete) + file.name)
                            .content(R.string.text_delete_confirm)
                            .positiveText(R.string.text_yes)
                            .onPositive { _, _ ->
                                if (file.delete()) {
                                    Helper.showSnackBar(activity!!.coordinator_layout, "Pointer deleted.")
                                } else {
                                    Helper.showSnackBar(activity!!.coordinator_layout, "Error deleting pointer.")
                                }
                            }
                            .negativeText(R.string.text_no)
                            .show()

                    false
                }
            } else {
                contentView.info_no_pointer_installed.visibility = View.VISIBLE
                contentView.bs_button_install_pointers.setOnClickListener {
                    MainActivity.showInstallPointerFragment(activity!!)
                }
            }
        } catch (npe: NullPointerException) {
            npe.printStackTrace()
        }
    }

    var mTitle: String? = null
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
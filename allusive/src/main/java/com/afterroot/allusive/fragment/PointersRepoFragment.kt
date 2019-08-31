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


import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.list.listItems
import com.afterroot.allusive.GlideApp
import com.afterroot.allusive.R
import com.afterroot.allusive.adapter.PointerAdapterDelegate
import com.afterroot.allusive.adapter.callback.ItemSelectedCallback
import com.afterroot.allusive.database.DatabaseFields
import com.afterroot.allusive.database.dbInstance
import com.afterroot.allusive.model.Pointer
import com.afterroot.allusive.utils.*
import com.afterroot.allusive.viewmodel.PointerViewModel
import com.afterroot.allusive.viewmodel.ViewModelState
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.fragment_pointer_info.view.*
import kotlinx.android.synthetic.main.fragment_pointer_repo.*
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.toast
import java.io.File

class PointersRepoFragment : Fragment(), ItemSelectedCallback {

    private lateinit var db: FirebaseFirestore
    private lateinit var extSdDir: String
    private lateinit var mTargetPath: String
    private lateinit var pointerAdapter: PointerAdapterDelegate
    private lateinit var pointersFolder: String
    private lateinit var pointersList: List<Pointer>
    private lateinit var pointersSnapshot: QuerySnapshot
    private lateinit var storage: FirebaseStorage
    private val pointerViewModel: PointerViewModel by lazy { ViewModelProvider(this).get(PointerViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        storage = FirebaseStorage.getInstance()
        db = dbInstance
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_pointer_repo, container, false)
    }

    override fun onResume() {
        super.onResume()

        if (!context!!.isNetworkAvailable()) {
            repo_swipe_refresh.visible(false)
            layout_no_network.visible(true)
            button_retry.setOnClickListener {
                onResume()
            }
            activity!!.fab_apply.hide()
        } else {
            activity!!.fab_apply.apply {
                show()
                setOnClickListener {
                    if (!context!!.isNetworkAvailable()) {
                        context!!.toast(getString(R.string.dialog_title_no_network))
                        return@setOnClickListener
                    }
                    activity!!.findNavController(R.id.fragment_repo_nav).navigate(R.id.newPostFragment)
                }
                icon = context!!.getDrawableExt(R.drawable.ic_add)
            }

            repo_swipe_refresh.visible(true)
            layout_no_network.visible(false)
            repo_swipe_refresh.apply {
                setOnRefreshListener {
                    loadPointers()
                }
                setColorSchemeResources(R.color.color_primary, R.color.color_secondary)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (FirebaseUtils.isUserSignedIn) {
            setUpList()

            pointersFolder = getString(R.string.pointer_folder_path)
            extSdDir = Environment.getExternalStorageDirectory().toString()
            mTargetPath = extSdDir + pointersFolder
        }
    }

    private fun setUpList() {
        pointerAdapter = PointerAdapterDelegate(this)
        list.apply {
            setHasFixedSize(true)
            val lm = LinearLayoutManager(context!!)
            layoutManager = lm
            addItemDecoration(DividerItemDecoration(this.context, lm.orientation))
            this.adapter = pointerAdapter
        }
        loadPointers()
    }

    private fun loadPointers() {
        pointerViewModel.getPointerSnapshot().observe(this, Observer<ViewModelState> {
            when (it) {
                is ViewModelState.Loading -> {
                    repo_swipe_refresh.isRefreshing = true
                }
                is ViewModelState.Loaded<*> -> {
                    repo_swipe_refresh.isRefreshing = false
                    pointersSnapshot = it.data as QuerySnapshot
                    pointersList = pointersSnapshot.toObjects()
                    pointerAdapter.add(pointersList)
                }
            }
        })
    }

    private fun showPointerInfoDialog(position: Int) {
        val dialog = MaterialDialog(context!!, BottomSheet(LayoutMode.MATCH_PARENT)).show {
            customView(R.layout.fragment_pointer_info, scrollable = true)
        }

        val pointer = pointersList[position]
        var isDownloaded = false
        val file = File("$mTargetPath${pointersList[position].filename}")
        if (file.exists()) {
            isDownloaded = true
        }

        dialog.getCustomView().apply {
            val storageReference =
                FirebaseStorage.getInstance().reference.child("${DatabaseFields.COLLECTION_POINTERS}/${pointer.filename}")
            info_pointer_pack_name.text = pointer.name
            info_pack_desc.text = pointer.description
            pointer.uploadedBy!!.forEach {
                info_username.text = String.format(context.getString(R.string.str_format_uploaded_by), it.value)
            }
            info_pointer_image.apply {
                background =
                    CheckeredDrawable().apply { alpha = context.resources.getInteger(R.integer.checkered_grid_alpha) }
                GlideApp.with(context).load(storageReference).override(128, 128).into(this)
            }
            info_action_pack.apply {
                if (isDownloaded) {
                    text = getString(R.string.text_installed)
                    setOnClickListener(null)
                    isEnabled = false
                } else {
                    text = getString(R.string.text_download)
                    setOnClickListener {
                        downloadPointer(position)
                        dialog.dismiss()
                    }
                    isEnabled = true
                }
            }
            info_tv_downloads_count.text = String.format(getString(R.string.str_format_download_count), pointer.downloads)
        }
    }

    private fun downloadPointer(position: Int) {
        val dialog = context!!.showStaticProgressDialog(getString(R.string.text_progress_downloading))
        val ref = storage.reference.child(DatabaseFields.COLLECTION_POINTERS).child(pointersList[position].filename)
        val file = File("$mTargetPath${pointersList[position].filename}")

        ref.getFile(file).addOnSuccessListener {
            activity!!.container.snackbar(getString(R.string.msg_pointer_downloaded)).anchorView = activity!!.navigation
            dialog.dismiss()
        }
        pointersSnapshot.documents[position].reference.update(
            DatabaseFields.FIELD_DOWNLOADS,
            pointersList[position].downloads + 1
        )
    }

    override fun onClick(position: Int, view: View?) {
        showPointerInfoDialog(position)
    }

    override fun onLongClick(position: Int) {
        val isIdMatch = pointersList[position].uploadedBy!!.containsKey(FirebaseUtils.firebaseUser!!.uid)
        if (!isIdMatch) return
        val list = arrayListOf(getString(R.string.text_edit), getString(R.string.text_delete))
        MaterialDialog(context!!, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            cornerRadius(16f)
            listItems(items = list) { _, _, text ->
                when (text) {
                    getString(R.string.text_edit) -> {
                        activity!!.container.snackbar("Will arrive soon.").anchorView = activity!!.navigation
                    }
                    getString(R.string.text_delete) -> {
                        MaterialDialog(context).show {
                            title(text = getString(R.string.dialog_title_confirmation))
                            icon(R.drawable.ic_delete)
                            positiveButton(android.R.string.yes) {
                                val filename = pointersList[position].filename
                                db.collection(DatabaseFields.COLLECTION_POINTERS)
                                    .whereEqualTo(DatabaseFields.FIELD_FILENAME, filename).get()
                                    .addOnSuccessListener { querySnapshot: QuerySnapshot? ->
                                        querySnapshot!!.documents.forEach { docSnapshot: DocumentSnapshot? ->
                                            docSnapshot!!.reference.delete().addOnSuccessListener {
                                                val ref = storage.reference.child(DatabaseFields.COLLECTION_POINTERS)
                                                    .child(filename)
                                                ref.delete().addOnSuccessListener {

                                                }
                                            }
                                        }
                                    }
                            }
                            negativeButton(android.R.string.no) {
                                it.dismiss()
                            }
                        }
                    }
                }
            }
        }
    }
}

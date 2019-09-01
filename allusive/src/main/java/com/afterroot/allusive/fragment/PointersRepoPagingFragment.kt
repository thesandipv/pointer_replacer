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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.paging.PagedList
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
import com.afterroot.allusive.adapter.MyFirestorePagingAdapter
import com.afterroot.allusive.adapter.callback.ItemSelectedCallback
import com.afterroot.allusive.database.DatabaseFields
import com.afterroot.allusive.database.dbInstance
import com.afterroot.allusive.model.Pointer
import com.afterroot.allusive.utils.*
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.fragment_pointer_info.view.*
import kotlinx.android.synthetic.main.fragment_pointer_repo.*
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.toast
import java.io.File

class PointersRepoPagingFragment : Fragment(), ItemSelectedCallback {

    private lateinit var db: FirebaseFirestore
    private lateinit var extSdDir: String
    private lateinit var mTargetPath: String
    private lateinit var myFirestorePagingAdapter: MyFirestorePagingAdapter
    private lateinit var pointersFolder: String
    private lateinit var storage: FirebaseStorage
    private val _tag = "PointersRepoFragment"

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
                    //loadPointers()
                    myFirestorePagingAdapter.refresh()
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
        val baseQuery = dbInstance.collection(DatabaseFields.COLLECTION_POINTERS)
            .orderBy(DatabaseFields.FIELD_TIME, Query.Direction.DESCENDING)

        val config =
            PagedList.Config.Builder().setEnablePlaceholders(false).setInitialLoadSizeHint(20).setPageSize(10).build()

        val options = FirestorePagingOptions.Builder<Pointer>().setLifecycleOwner(this)
            .setQuery(baseQuery, config, Pointer::class.java).build()

        //pointerAdapter = PointerAdapterDelegate(this)
        myFirestorePagingAdapter = MyFirestorePagingAdapter(options, this)
        list.apply {
            val lm = LinearLayoutManager(context!!)
            layoutManager = lm
            addItemDecoration(DividerItemDecoration(this.context, lm.orientation))
            this.adapter = myFirestorePagingAdapter
        }
    }

    private fun getPointer(position: Int) =
        myFirestorePagingAdapter.currentList!![position]?.toObject(Pointer::class.java)

    private fun showPointerInfoDialog(position: Int) {
        val dialog = MaterialDialog(context!!, BottomSheet(LayoutMode.MATCH_PARENT)).show {
            customView(R.layout.fragment_pointer_info, scrollable = true)
        }

        val pointer = getPointer(position)!!
        Log.d(_tag, "showPointerInfoDialog: $pointer")
        var isDownloaded = false
        val file = File("$mTargetPath${pointer.filename}")
        if (file.exists()) isDownloaded = true

        dialog.getCustomView().apply {
            val storageReference =
                FirebaseStorage.getInstance().reference.child("${DatabaseFields.COLLECTION_POINTERS}/${pointer.filename}")
            info_pointer_pack_name.text = pointer.name
            info_pack_desc.text = pointer.description
            pointer.uploadedBy!!.forEach {
                info_username.text = String.format(context.getString(R.string.str_format_uploaded_by), it.value)
            }
            info_pointer_image.apply {
                context!!.toast("isAvailable: ${pointer.reasonCode}")
                if (pointer.reasonCode <= 0) {
                    background = context.getDrawableExt(R.drawable.transparent_grid)
                    GlideApp.with(context)
                        .load(storageReference)
                        .override(128, 128).into(this)
                } else {
                    background = null
                    setImageDrawable(context.getDrawableExt(R.drawable.ic_removed, R.color.color_error))
                }

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
        val ref = storage.reference.child(DatabaseFields.COLLECTION_POINTERS).child(getPointer(position)!!.filename)
        val file = File("$mTargetPath${getPointer(position)!!.filename}")

        ref.getFile(file).addOnSuccessListener {
            activity!!.container.snackbar(getString(R.string.msg_pointer_downloaded)).anchorView = activity!!.navigation
            dialog.dismiss()
        }.addOnFailureListener {
            activity!!.container.snackbar("Pointer not Available").anchorView = activity!!.navigation
            dialog.dismiss()
        }

        myFirestorePagingAdapter.currentList!![position]!!.reference.update(
            DatabaseFields.FIELD_DOWNLOADS,
            getPointer(position)!!.downloads + 1
        )
    }

    override fun onClick(position: Int, view: View?) {
        showPointerInfoDialog(position)
    }

    override fun onLongClick(position: Int) {
        val isIdMatch = getPointer(position)!!.uploadedBy!!.containsKey(FirebaseUtils.firebaseUser!!.uid)
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
                                val filename = getPointer(position)!!.filename
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

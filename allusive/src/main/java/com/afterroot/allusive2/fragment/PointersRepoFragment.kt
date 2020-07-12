/*
 * Copyright (C) 2016-2020 Sandip Vaghela
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

package com.afterroot.allusive2.fragment


import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.list.listItems
import com.afterroot.allusive2.BuildConfig
import com.afterroot.allusive2.GlideApp
import com.afterroot.allusive2.R
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.adapter.PointerAdapterDelegate
import com.afterroot.allusive2.adapter.callback.ItemSelectedCallback
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.database.MyDatabase
import com.afterroot.allusive2.model.Pointer
import com.afterroot.allusive2.model.RoomPointer
import com.afterroot.allusive2.utils.FirebaseUtils
import com.afterroot.allusive2.viewmodel.PointerRepoViewModel
import com.afterroot.allusive2.viewmodel.ViewModelState
import com.afterroot.core.extensions.getDrawableExt
import com.afterroot.core.extensions.isNetworkAvailable
import com.afterroot.core.extensions.showStaticProgressDialog
import com.afterroot.core.extensions.visible
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.fragment_pointer_info.view.*
import kotlinx.android.synthetic.main.fragment_pointer_repo.*
import kotlinx.android.synthetic.main.fragment_pointer_repo.view.*
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.toast
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import java.io.File

class PointersRepoFragment : Fragment(), ItemSelectedCallback {

    private lateinit var extSdDir: String
    private lateinit var mTargetPath: String
    private lateinit var pointerAdapter: PointerAdapterDelegate
    private lateinit var pointersFolder: String
    private lateinit var pointersList: List<Pointer>
    private lateinit var pointersSnapshot: QuerySnapshot
    private lateinit var settings: Settings
    private val db: FirebaseFirestore by inject()
    private val myDatabase: MyDatabase by inject()
    private val pointerViewModel: PointerRepoViewModel by viewModels()
    private val storage: FirebaseStorage by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_pointer_repo, container, false)
    }

    override fun onResume() {
        super.onResume()
        onNetworkChange(requireContext().isNetworkAvailable())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (FirebaseUtils.isUserSignedIn) {
            settings = Settings(requireContext())
            pointersFolder = getString(R.string.pointer_folder_path)
            extSdDir = Environment.getExternalStorageDirectory().toString()
            mTargetPath = extSdDir + pointersFolder
            setUpList()
        }
    }

    private fun onNetworkChange(isAvailable: Boolean) {
        if (!isAvailable) {
            repo_swipe_refresh.visible(false)
            layout_no_network.visible(true)
            button_retry.setOnClickListener {
                onResume()
            }
            requireActivity().fab_apply.hide()
        } else {
            requireActivity().fab_apply.apply {
                show()
                setOnClickListener {
                    if (!requireContext().isNetworkAvailable()) {
                        requireContext().toast(getString(R.string.dialog_title_no_network))
                        return@setOnClickListener
                    }
                    requireActivity().findNavController(R.id.fragment_repo_nav).navigate(R.id.newPostFragment)
                }
                icon = requireContext().getDrawableExt(R.drawable.ic_add)
            }

            repo_swipe_refresh.visible(true)
            layout_no_network.visible(false)
            repo_swipe_refresh.apply {
                setOnRefreshListener {
                    try {
                        setUpList()
                    } catch (e: IllegalStateException) {
                        isRefreshing = false
                        e.printStackTrace()
                    }
                }
                setColorSchemeResources(R.color.color_primary, R.color.color_secondary)
            }
        }
    }

    private fun setUpList() {
        pointerAdapter = PointerAdapterDelegate(this, getKoin())
        list.apply {
            val lm = LinearLayoutManager(requireContext())
            layoutManager = lm
            addItemDecoration(DividerItemDecoration(this.context, lm.orientation))
        }
        loadPointers()
        setUpFilter()
    }

    private fun loadPointers(orderBy: String = settings.orderBy!!) {
        pointerViewModel.getPointerSnapshot(orderBy).observe(viewLifecycleOwner, Observer {
            when (it) {
                is ViewModelState.Loading -> {
                    repo_swipe_refresh.isRefreshing = true
                }
                is ViewModelState.Loaded<*> -> {
                    repo_swipe_refresh.isRefreshing = false
                    pointersSnapshot = it.data as QuerySnapshot
                    pointersList = pointersSnapshot.toObjects()
                    pointerAdapter.add(pointersList)
                    list.adapter = pointerAdapter
                    list.scheduleLayoutAnimation()
                }
            }
        })
    }

    private fun setUpFilter() {
        requireView().repo_filter_chip_group.apply {
            clearCheck()
            when (settings.orderBy) {
                DatabaseFields.FIELD_TIME -> this.check(R.id.filter_chip_sort_by_date)
                DatabaseFields.FIELD_DOWNLOADS -> this.check(R.id.filter_chip_sort_by_download)
            }
        }
        requireView().filter_chip_sort_by_date.setOnClickListener {
            loadPointers(DatabaseFields.FIELD_TIME)
            requireView().repo_filter_chip_group.apply {
                clearCheck()
                check(R.id.filter_chip_sort_by_date)
                settings.orderBy = DatabaseFields.FIELD_TIME
            }
        }
        requireView().filter_chip_sort_by_download.setOnClickListener {
            loadPointers(DatabaseFields.FIELD_DOWNLOADS)
            requireView().repo_filter_chip_group.apply {
                clearCheck()
                check(R.id.filter_chip_sort_by_download)
                settings.orderBy = DatabaseFields.FIELD_DOWNLOADS
            }
        }
    }

    private fun showPointerInfoDialog(position: Int) {
        val dialog = MaterialDialog(requireContext(), BottomSheet(LayoutMode.MATCH_PARENT)).show {
            customView(R.layout.fragment_pointer_info, scrollable = true)
        }

        val pointer = pointersList[position]

        dialog.getCustomView().apply {
            val storageReference =
                FirebaseStorage.getInstance().reference.child("${DatabaseFields.COLLECTION_POINTERS}/${pointer.filename}")
            info_pointer_pack_name.text = pointer.name
            info_pack_desc.text = pointer.description
            pointer.uploadedBy!!.forEach {
                info_username.text = String.format(context.getString(R.string.str_format_uploaded_by), it.value)
            }
            info_pointer_image.apply {
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
                lifecycleScope.launch {
                    if (myDatabase.pointerDao().exists(pointer.filename).isNotEmpty()) {
                        text = getString(R.string.text_installed)
                        setOnClickListener(null)
                        isEnabled = false
                    } else {
                        text = getString(R.string.text_download)
                        setOnClickListener {
                            downloadPointer(position)
                            dialog.dismiss()
                        }
                        isEnabled = pointer.reasonCode == 0
                    }
                }
            }
            info_tv_downloads_count.text =
                resources.getQuantityString(R.plurals.str_format_download_count, pointer.downloads, pointer.downloads)
            /*info_rate_up.setOnClickListener {
                pointersSnapshot.documents[position].reference.update(
                    DatabaseFields.FIELD_UPVOTES,
                    pointersList[position].upvotes + 1
                )
            }
            info_rate_down.setOnClickListener {

            }*/
        }
    }

    private fun downloadPointer(position: Int) {
        val dialog = requireContext().showStaticProgressDialog(getString(R.string.text_progress_downloading))
        val ref = storage.reference.child(DatabaseFields.COLLECTION_POINTERS).child(pointersList[position].filename)
        ref.getFile(File("$mTargetPath${pointersList[position].filename}"))
            .addOnSuccessListener {
                requireActivity().container.snackbar(getString(R.string.msg_pointer_downloaded)).anchorView =
                    requireActivity().navigation
                if (!BuildConfig.DEBUG) {
                    pointersSnapshot.documents[position].reference.update(
                        DatabaseFields.FIELD_DOWNLOADS,
                        pointersList[position].downloads + 1
                    )
                }
                val p = pointersList[position]
                var id = ""
                var name = ""
                p.uploadedBy!!.forEach {
                    id = it.key
                    name = it.value
                }
                val pointer = RoomPointer(
                    file_name = p.filename,
                    pointer_desc = p.description,
                    pointer_name = p.name,
                    uploader_id = id,
                    uploader_name = name
                )
                lifecycleScope.launch {
                    myDatabase.pointerDao().add(pointer)
                }

                dialog.dismiss()
            }.addOnFailureListener {
                requireActivity().container.snackbar(getString(R.string.msg_error)).anchorView =
                    requireActivity().navigation
                dialog.dismiss()
            }
    }

    override fun onClick(position: Int, view: View?) {
        showPointerInfoDialog(position)
    }

    override fun onLongClick(position: Int) {
        val isIdMatch = if (BuildConfig.DEBUG) true
        else pointersList[position].uploadedBy!!.containsKey(FirebaseUtils.firebaseUser!!.uid)
        if (!isIdMatch) return
        val list = arrayListOf(getString(R.string.text_edit), getString(R.string.text_delete))
        MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            cornerRadius(16f)
            listItems(items = list) { _, _, text ->
                when (text) {
                    getString(R.string.text_edit) -> {
                        requireActivity().container.snackbar("Will arrive soon.").anchorView = requireActivity().navigation
                    }
                    getString(R.string.text_delete) -> {
                        MaterialDialog(context).show {
                            message(text = getString(R.string.dialog_delete_confirm))
                            positiveButton(R.string.text_delete) {
                                val filename = pointersList[position].filename
                                db.collection(DatabaseFields.COLLECTION_POINTERS)
                                    .whereEqualTo(DatabaseFields.FIELD_FILENAME, filename).get()
                                    .addOnSuccessListener { querySnapshot: QuerySnapshot? ->
                                        querySnapshot!!.documents.forEach { docSnapshot: DocumentSnapshot? ->
                                            docSnapshot!!.reference.delete().addOnSuccessListener {
                                                //go to last position after deleting pointer
                                                this@PointersRepoFragment.list.scrollToPosition(position)
                                                //delete pointer from storage bucket
                                                storage.reference.child(DatabaseFields.COLLECTION_POINTERS).child(filename)
                                                    .delete()
                                                context.toast(R.string.msg_delete_success)
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

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
package com.afterroot.allusive2.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.paging.LoadState
import androidx.paging.PagingData
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
import com.afterroot.allusive2.Reason
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.adapter.PointersAdapter
import com.afterroot.allusive2.adapter.callback.ItemSelectedCallback
import com.afterroot.allusive2.database.DatabaseFields
import com.afterroot.allusive2.database.MyDatabase
import com.afterroot.allusive2.databinding.DialogEditPointerBinding
import com.afterroot.allusive2.databinding.FragmentPointerInfoBinding
import com.afterroot.allusive2.databinding.FragmentPointerRepoBinding
import com.afterroot.allusive2.getPointerSaveDir
import com.afterroot.allusive2.model.Pointer
import com.afterroot.allusive2.model.RoomPointer
import com.afterroot.allusive2.repo.PointerPagingAdapter
import com.afterroot.allusive2.utils.FirebaseUtils
import com.afterroot.allusive2.viewmodel.MainSharedViewModel
import com.afterroot.allusive2.viewmodel.NetworkViewModel
import com.afterroot.allusive2.viewmodel.ViewModelState
import com.afterroot.core.extensions.getDrawableExt
import com.afterroot.core.extensions.showStaticProgressDialog
import com.afterroot.core.extensions.visible
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.jetbrains.anko.doFromSdk
import org.jetbrains.anko.toast
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.text.SimpleDateFormat

class PointersRepoFragment : Fragment(), ItemSelectedCallback<Pointer> {

    private lateinit var binding: FragmentPointerRepoBinding
    private lateinit var pointersAdapter: PointersAdapter
    private lateinit var pointersPagingAdapter: PointerPagingAdapter
    private lateinit var pointersList: List<Pointer>
    private lateinit var pointersSnapshot: QuerySnapshot
    private lateinit var targetPath: String
    private lateinit var fabApply: ExtendedFloatingActionButton
    private val firestore: FirebaseFirestore by inject()
    private val myDatabase: MyDatabase by inject()
    private val networkViewModel: NetworkViewModel by viewModel()
    private val settings: Settings by inject()
    private val sharedViewModel: MainSharedViewModel by viewModel()
    private val storage: FirebaseStorage by inject()
    private var filteredList: List<Pointer>? = null
    private val firebaseUtils: FirebaseUtils by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPointerRepoBinding.inflate(inflater, container, false)
        fabApply = requireActivity().findViewById(R.id.fab_apply)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        networkViewModel.monitor(
            this,
            onConnect = {
                onNetworkChange(true)
            },
            onDisconnect = {
                onNetworkChange(false)
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (firebaseUtils.isUserSignedIn) {
            targetPath = requireContext().getPointerSaveDir()
            // setUpList()
            setUpAdapter()
            loadPointers()
        }
    }

    private fun onNetworkChange(isAvailable: Boolean) {
        binding.repoSwipeRefresh.visible(isAvailable)
        binding.layoutNoNetwork.visible(!isAvailable)
        if (!isAvailable) {
            binding.buttonRetry.setOnClickListener {
                onResume()
            }
            fabApply.hide()
        } else {
            fabApply.apply {
                show()
                setOnClickListener {
                    requireActivity().findNavController(R.id.fragment_repo_nav).navigate(R.id.repo_to_new_pointer)
                }
                icon = requireContext().getDrawableExt(R.drawable.ic_add)
            }

            binding.repoSwipeRefresh.apply {
                setOnRefreshListener {
                    try {
                        loadPointers()
                    } catch (e: IllegalStateException) {
                        isRefreshing = false
                        e.printStackTrace()
                    }
                }
                setColorSchemeResources(R.color.color_primary, R.color.color_secondary)
            }
        }
    }

    // Function for using new list adapter.
    private fun setUpAdapter() {
        pointersAdapter = PointersAdapter(this)
        pointersPagingAdapter = PointerPagingAdapter(this)
        binding.list.apply {
            val lm = LinearLayoutManager(requireContext())
            layoutManager = lm
            addItemDecoration(DividerItemDecoration(this.context, lm.orientation))
            adapter = pointersPagingAdapter
            setHasFixedSize(true)
            doFromSdk(Build.VERSION_CODES.LOLLIPOP) { FastScrollerBuilder(this).build() }
        }
        setUpFilter()
    }

    private fun displayPointers(pointers: List<Pointer> = emptyList(), pagedData: PagingData<Pointer>? = null) {
        if (pagedData == null) {
            pointersList = pointers
            pointersAdapter.submitList(pointers)
        } else {
            lifecycleScope.launch {
                pointersPagingAdapter.submitData(pagedData)
                pointersPagingAdapter
            }
        }
    }

    private fun loadPointers(orderBy: String = settings.orderBy!!) {
        val enablePaged = true
        if (enablePaged) {
            lifecycleScope.launch {
                sharedViewModel.pointers.collect {
//                    pointersPagingAdapter.submitData(it)
                    displayPointers(pagedData = it)
                }
            }

            lifecycleScope.launch {
                pointersPagingAdapter.loadStateFlow.collectLatest {
                    binding.repoSwipeRefresh.isRefreshing = it.refresh is LoadState.Loading || it.append is LoadState.Loading
                }
            }
        } else {
            sharedViewModel.getPointerSnapshot().observe(
                viewLifecycleOwner,
                { state ->
                    when (state) {
                        is ViewModelState.Loading -> {
                            binding.repoSwipeRefresh.isRefreshing = true
                        }
                        is ViewModelState.Loaded<*> -> {
                            binding.repoSwipeRefresh.isRefreshing = false
                            pointersSnapshot = state.data as QuerySnapshot
                            val result: List<Pointer> = pointersSnapshot.toObjects()
                            val currOrder = if (orderBy == settings.orderBy) orderBy else settings.orderBy
                            pointersList = if (currOrder == DatabaseFields.FIELD_TIME) {
                                result.sortedByDescending { it.time }
                            } else {
                                result.sortedByDescending { it.downloads }
                            }
                            displayPointers(pointersList)
                        }
                    }
                }
            )

        }
    }

    private fun setUpFilter() {
        binding.repoSortChipGroup.apply {
            when (settings.orderBy) {
                DatabaseFields.FIELD_TIME -> this.check(R.id.filter_chip_sort_by_date)
                DatabaseFields.FIELD_DOWNLOADS -> this.check(R.id.filter_chip_sort_by_download)
            }
        }

        binding.filterChipSortByDate.apply {
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    displayPointers(
                        (filteredList ?: pointersList).sortedByDescending {
                            it.time
                        }
                    )
                    settings.orderBy = DatabaseFields.FIELD_TIME
                }
            }
        }
        binding.filterChipSortByDownload.apply {
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    displayPointers(
                        (filteredList ?: pointersList).sortedByDescending {
                            it.downloads
                        }
                    )
                    settings.orderBy = DatabaseFields.FIELD_DOWNLOADS
                }
            }
        }

        binding.filterChipShowUserUploaded.apply {
            setOnCheckedChangeListener { _, isChecked ->
                isCloseIconVisible = isChecked
                if (isChecked) {
                    binding.repoSwipeRefresh.isEnabled = false
                    filteredList = pointersList.filter {
                        it.uploadedBy?.containsKey(get<FirebaseAuth>().currentUser?.uid) ?: false
                    }
                    displayPointers(filteredList ?: pointersList)
                } else {
                    filteredList = null
                    binding.repoSwipeRefresh.isEnabled = true
                    loadPointers()
                }
            }
            setOnCloseIconClickListener {
                isChecked = false
            }
        }
    }

    private fun showPointerInfoDialog(pointer: Pointer) {
        // FIXME check this is working or not
        val binding = FragmentPointerInfoBinding.inflate(layoutInflater)
        val dialog = MaterialDialog(requireContext(), BottomSheet(LayoutMode.MATCH_PARENT)).show {
            customView(view = binding.root, scrollable = true)
        }

        dialog.getCustomView().apply {
            when (pointer.reasonCode) {
                Reason.OK -> {
                    binding.pointer = pointer
                    binding.infoPointerImage.apply {
                        updateLayoutParams<ConstraintLayout.LayoutParams> {
                            height = 128
                            width = 128
                        }
                        val storageReference = get<FirebaseStorage>().reference
                            .child("${DatabaseFields.COLLECTION_POINTERS}/${pointer.filename}")
                        val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
                        background = context.getDrawableExt(R.drawable.transparent_grid)
                        GlideApp.with(context)
                            .load(storageReference)
                            .override(128, 128)
                            .transition(DrawableTransitionOptions.withCrossFade(factory))
                            .into(this)
                    }
                    binding.infoActionPack.apply {
                        lifecycleScope.launch {
                            if (myDatabase.pointerDao().exists(pointer.filename!!).isNotEmpty()) {
                                text = getString(R.string.text_installed)
                                setOnClickListener(null)
                                isEnabled = false
                            } else {
                                text = getString(R.string.text_download)
                                setOnClickListener {
                                    downloadPointer(pointer)
                                    dialog.dismiss()
                                }
                                isEnabled = pointer.reasonCode == Reason.OK
                            }
                        }
                    }
                    val downloads = resources.getQuantityString(
                        R.plurals.str_format_download_count,
                        pointer.downloads,
                        pointer.downloads
                    )
                    val date = SimpleDateFormat.getDateInstance().format(pointer.time)
                    binding.downloadsText =
                        String.format(getString(R.string.format_text_downloads_and_date), downloads, date)
                    pointer.uploadedBy?.forEach {
                        binding.uploadedBy = String.format(context.getString(R.string.str_format_uploaded_by), it.value)
                    }
                }
                else -> {
                    binding.infoPointerImage.apply {
                        updateLayoutParams<ConstraintLayout.LayoutParams> {
                            height = 128
                            width = 128
                        }
                        background = null
                        setImageDrawable(context.getDrawableExt(R.drawable.ic_removed, R.color.color_error))
                    }
                    binding.apply {
                        this.pointer = pointer
                        downloadsText = null
                        uploadedBy = null
                        infoActionPack.visible(false)
                    }
                }
            }
        }
    }

    private fun downloadPointer(pointer: Pointer) {
        val dialog = requireContext().showStaticProgressDialog(getString(R.string.text_progress_downloading))
        val ref = storage.reference.child(DatabaseFields.COLLECTION_POINTERS).child(pointer.filename!!)
        ref.getFile(File("$targetPath${pointer.filename}"))
            .addOnSuccessListener {
                sharedViewModel.displayMsg(getString(R.string.msg_pointer_downloaded))
                if (!BuildConfig.DEBUG) {
                    pointersSnapshot.query.whereEqualTo(DatabaseFields.FIELD_FILENAME, pointer.filename).get(Source.CACHE)
                        .addOnSuccessListener {
                            val docId = it.documents.first().id
                            firestore.collection(DatabaseFields.COLLECTION_POINTERS).document(docId).update(
                                DatabaseFields.FIELD_DOWNLOADS,
                                pointer.downloads + 1
                            )
                        }
                }
                var id = ""
                var name = ""
                pointer.uploadedBy!!.forEach {
                    id = it.key
                    name = it.value
                }
                val roomPointer = RoomPointer(
                    file_name = pointer.filename,
                    pointer_desc = pointer.description,
                    pointer_name = pointer.name,
                    uploader_id = id,
                    uploader_name = name
                )
                lifecycleScope.launch {
                    myDatabase.pointerDao().add(roomPointer)
                }

                dialog.dismiss()
            }.addOnFailureListener {
                sharedViewModel.displayMsg(getString(R.string.msg_error))
                dialog.dismiss()
            }
    }

    private fun showEditPointerDialog(pointer: Pointer) {
        val binding = DialogEditPointerBinding.inflate(layoutInflater)
        val dialog = MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT))
        dialog.show {
            customView(view = binding.root)
            title(text = "Edit Pointer Info")
            val oldTitle = pointer.name
            val oldDesc = pointer.description
            binding.tiTitle.setText(oldTitle)
            binding.tiDesc.setText(oldDesc)
            positiveButton(res = R.string.text_action_save) {
                val newTitle = binding.tiTitle.text.toString()
                val newDesc = binding.tiDesc.text.toString()
                if (oldTitle != newTitle || oldDesc != newDesc) {
                    pointersSnapshot.query.whereEqualTo(DatabaseFields.FIELD_FILENAME, pointer.filename).get(Source.CACHE)
                        .addOnSuccessListener {
                            val docId = it.documents.first().id
                            val updates = mapOf(
                                Pair(DatabaseFields.FIELD_NAME, newTitle),
                                Pair(DatabaseFields.FIELD_DESC, newDesc)
                            )
                            firestore.collection(DatabaseFields.COLLECTION_POINTERS).document(docId).update(updates)
                        }
                } else {
                    // No changes to save. dismiss dialog.
                    dismiss()
                }
            }
        }
    }

    override fun onClick(position: Int, view: View?, item: Pointer) {
        showPointerInfoDialog(item)
    }

    override fun onLongClick(position: Int, item: Pointer): Boolean {
        val result = kotlin.runCatching {
            if (!item.uploadedBy!!.containsKey(firebaseUtils.uid) || item.reasonCode != Reason.OK) {
                return false
            }
            val list = mutableListOf(getString(R.string.text_edit), getString(R.string.text_delete))
            MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                cornerRadius(16f)
                listItems(items = list) { _, _, text ->
                    when (text) {
                        getString(R.string.text_edit) -> {
                            showEditPointerDialog(item)
                        }
                        getString(R.string.text_delete) -> {
                            MaterialDialog(context).show {
                                message(text = getString(R.string.dialog_delete_confirm))
                                positiveButton(R.string.text_delete) {
                                    val filename = item.filename
                                    firestore.collection(DatabaseFields.COLLECTION_POINTERS)
                                        .whereEqualTo(DatabaseFields.FIELD_FILENAME, filename).get()
                                        .addOnSuccessListener { querySnapshot: QuerySnapshot? ->
                                            querySnapshot!!.documents.forEach { docSnapshot: DocumentSnapshot? ->
                                                docSnapshot!!.reference.delete().addOnSuccessListener {
                                                    // go to last position after deleting pointer
                                                    binding.list.scrollToPosition(position)
                                                    // delete pointer from storage bucket
                                                    storage.reference.child(DatabaseFields.COLLECTION_POINTERS)
                                                        .child(filename!!)
                                                        .delete()
                                                    context.toast(R.string.msg_delete_success)
                                                }
                                            }
                                        }
                                }
                                negativeButton(android.R.string.cancel) {
                                    it.dismiss()
                                }
                            }
                        }
                    }
                }
            }
        }
        result.onFailure {
            it.message?.let { it1 -> requireContext().toast(it1) }
        }
        return true
    }

    companion object {
        @Suppress("unused")
        private const val TAG = "PointersRepoFragment"
    }
}

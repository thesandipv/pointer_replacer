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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afterroot.allusive.R
import com.afterroot.allusive.adapter.PointerAdapterDelegate
import com.afterroot.allusive.adapter.callback.ItemSelectedCallback
import com.afterroot.allusive.model.Pointer
import com.afterroot.allusive.utils.FirebaseUtils
import com.afterroot.allusive.utils.getDrawableExt
import com.afterroot.allusive.viewmodel.PointerViewModel
import com.afterroot.allusive.viewmodel.ViewModelState
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.fragment_pointer_repo.*
import org.jetbrains.anko.design.snackbar
import java.io.File

class PointersRepoFragment : Fragment(), ItemSelectedCallback {

    private var pointerAdapter: PointerAdapterDelegate? = null
    private lateinit var storage: FirebaseStorage
    private val pointerViewModel: PointerViewModel by lazy { ViewModelProviders.of(this).get(PointerViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        storage = FirebaseStorage.getInstance()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_pointer_repo, container, false)
    }

    override fun onResume() {
        super.onResume()

        activity!!.fab_apply.apply {
            setOnClickListener {
                activity!!.findNavController(R.id.fragment_repo_nav).navigate(R.id.newPostFragment)
            }
            icon = context!!.getDrawableExt(R.drawable.ic_add)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (FirebaseUtils.isUserSignedIn) {
            loadPointers()
        }
    }


    lateinit var pointersList: List<Pointer>
    private fun loadPointers() {
        pointerAdapter = PointerAdapterDelegate(this)
        list.apply {
            val lm = LinearLayoutManager(context!!)
            layoutManager = lm
            addItemDecoration(DividerItemDecoration(this.context, lm.orientation))
            this.adapter = pointerAdapter
        }

        pointerViewModel.getPointerSnapshot().observe(this, Observer<ViewModelState> {
            when (it) {
                is ViewModelState.Loading -> {

                }
                is ViewModelState.Loaded<*> -> {
                    pointersList = (it.data as QuerySnapshot).toObjects(Pointer::class.java)
                    pointerAdapter!!.add(pointersList)
                }
            }
        })
    }

    override fun onClick(position: Int, view: View?) {
        when (view!!.id) {
            R.id.item_action_pack -> {
                Toast.makeText(context!!, pointersList[position].name, Toast.LENGTH_SHORT).show()

                val pointersFolder = getString(R.string.pointer_folder_path)
                val extSdDir = Environment.getExternalStorageDirectory().toString()
                val mTargetPath = extSdDir + pointersFolder

                val ref = storage.reference.child("pointers").child(pointersList[position].filename)

                val file = File("$mTargetPath${pointersList[position].filename}")

                ref.getFile(file).addOnSuccessListener {
                    activity!!.container.snackbar("Pointer Installed").anchorView = activity!!.navigation
                }
            }
        }
    }

    override fun onLongClick(position: Int) {
    }

}

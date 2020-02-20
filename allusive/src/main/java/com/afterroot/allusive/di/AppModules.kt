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

package com.afterroot.allusive.di

import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.room.Room
import com.afterroot.allusive.Settings
import com.afterroot.allusive.database.MyDatabase
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val roomModule = module {
    single {
        Room.databaseBuilder(
            androidApplication(),
            MyDatabase::class.java,
            "installed-pointers"
        ).build()
    }

    single { get<MyDatabase>().pointerDao() }
}

val firebaseModule = module {
    single {
        Firebase.firestore.apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
        }
    }

    single {
        FirebaseStorage.getInstance()
    }
}

val appModule = module {
    single {
        Settings(androidContext())
    }
}

val documentModule = module {
    single {
        Settings(androidContext()).safUri?.toUri()
    }
    single {
        DocumentFile.fromTreeUri(
            androidContext(),
            get()
        )?.findFile("Pointer Replacer")?.findFile("Pointers")
    }
}
package com.afterroot.allusive2.magisk

import android.content.Context
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.IInStream
import net.sf.sevenzipjbinding.IOutCreateCallback
import net.sf.sevenzipjbinding.IOutItemAllFormats
import net.sf.sevenzipjbinding.IOutUpdateArchive
import net.sf.sevenzipjbinding.ISequentialInStream
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.SevenZipException
import net.sf.sevenzipjbinding.impl.OutItemFactory
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream
import net.sf.sevenzipjbinding.util.ByteArrayStream
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile

/*
 * Copyright (C) 2016-2022 Sandip Vaghela
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
class UpdateArchive(val context: Context) {
    /**
     * The callback defines the modification to be made.
     */
    private inner class MyCreateCallback : IOutCreateCallback<IOutItemAllFormats?> {
        @Throws(SevenZipException::class)
        override fun setOperationResult(operationResultOk: Boolean) {
            // Track each operation result here
        }

        @Throws(SevenZipException::class)
        override fun setTotal(total: Long) {
            // Track operation progress here
        }

        @Throws(SevenZipException::class)
        override fun setCompleted(complete: Long) {
            // Track operation progress here
        }

        @Throws(SevenZipException::class)
        override fun getItemInformation(
            index: Int,
            outItemFactory: OutItemFactory<IOutItemAllFormats?>?
        ): IOutItemAllFormats? {
            if (index == itemToAdd) {
                // Adding new item
                val outItem = outItemFactory?.createOutItem()
                outItem?.propertyPath = itemToAddPath
                outItem?.dataSize = itemToAddContent.size.toLong()
                return outItem
            }

            // Remove item by changing the mapping "new index"->"old index"
            return if (index < itemToRemove) {
                outItemFactory?.createOutItem(index)
            } else outItemFactory?.createOutItem(index + 1)
        }

        @Throws(SevenZipException::class)
        override fun getStream(i: Int): ISequentialInStream? {
            return if (i != itemToAdd) {
                null
            } else ByteArrayStream(itemToAddContent, true)
        }
    }

    var itemToAdd = 0 // New index of the item to add
    var itemToAddPath: String? = null
    lateinit var itemToAddContent: ByteArray
    var itemToRemove = 0 // Old index of the item to be removed

    @Throws(SevenZipException::class)
    private fun initUpdate(inArchive: IInArchive) {
        itemToAdd = inArchive.numberOfItems - 1
        itemToAddPath = POINTER_XHDPI
        itemToAddContent = File("${frameworkExtractPath(context)}$POINTER_XHDPI").readBytes()
        itemToRemove = -1
        for (i in 0 until inArchive.numberOfItems) {
            if (inArchive.getProperty(i, PropID.PATH) == POINTER_XHDPI.substring(1)) {
                itemToRemove = i
                break
            }
        }
        if (itemToRemove == -1) {
            throw RuntimeException("Item 'info.txt' not found")
        }
    }

    fun compress(`in`: String, out: String): File {
        var success = false
        var inRaf: RandomAccessFile? = null
        var outRaf: RandomAccessFile? = null
        val inArchive: IInArchive
        var outArchive: IOutUpdateArchive<IOutItemAllFormats?>? = null
        val closeables: MutableList<Closeable> = ArrayList()
        try {
            // Open input file
            inRaf = RandomAccessFile(`in`, "r")
            closeables.add(inRaf)
            val inStream: IInStream = RandomAccessFileInStream(inRaf)

            // Open in-archive
            inArchive = SevenZip.openInArchive(null, inStream)
            closeables.add(inArchive)
            initUpdate(inArchive)
            outRaf = RandomAccessFile(out, "rw")
            closeables.add(outRaf)

            // Open out-archive object
            outArchive = inArchive.connectedOutArchive

            // Modify archive
            outArchive.updateItems(
                RandomAccessFileOutStream(outRaf),
                inArchive.numberOfItems, MyCreateCallback()
            )
            success = true
        } catch (e: SevenZipException) {
            Timber.e("compress: 7z-Error occurs:")
            // Get more information using extended method
            e.printStackTraceExtended()
        } catch (e: Exception) {
            Timber.e("compress: Error occurs: $e")
        } finally {
            for (i in closeables.indices.reversed()) {
                try {
                    closeables[i].close()
                } catch (e: Throwable) {
                    Timber.e("compress: Error closing resource: $e")
                    success = false
                }
            }
        }
        if (success) {
            Timber.d("compress: success")
        }
        return File(out)
    }
}

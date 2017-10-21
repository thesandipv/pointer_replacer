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
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import com.afterroot.allusive.Helper
import com.afterroot.allusive.R
import kotlinx.android.synthetic.main.fragment_customize_pointer.*
import kotlinx.android.synthetic.main.fragment_main.*
import java.util.*

/**
 * Created by Sandip on 04-10-2017.
 */
class CustomizeFragment: Fragment() {
    private var mFragmentView: View? = null
    private var mSharedPreferences : SharedPreferences? = null
    var mEditor: SharedPreferences.Editor? = null


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mFragmentView = inflater?.inflate(R.layout.fragment_customize_pointer, container, false)
        return mFragmentView
    }

    @SuppressLint("CommitPrefEdits")
    override fun onStart() {
        super.onStart()

        mSharedPreferences = Helper.getSharedPreferences(activity)
        mEditor = mSharedPreferences!!.edit()
    }

    private val minSize: Int
        get() = if (Helper.getDpi(activity) <= 240) {
            49
        } else {
            66
        }

    /**
     * Set initial values to SeekBar
     */
    private fun setSeekBars() {
        val maxSize = mSharedPreferences!!.getInt(getString(R.string.key_maxPointerSize), 100)
        val maxPadding = mSharedPreferences!!.getInt(getString(R.string.key_maxPaddingSize), 25)
        val alpha = mSharedPreferences!!.getInt("pointerAlpha", 255)
        val pointerSize = mSharedPreferences!!.getInt(getString(R.string.key_pointerSize), minSize)
        val padding = mSharedPreferences!!.getInt(getString(R.string.key_pointerPadding), 0)
        val formatTextSize = "%s: %d*%d "
        val formatPadding = "| %s: %d "
        val step = 1

//        seekBarSize.min = minSize

        if (mSharedPreferences!!.getBoolean(getString(R.string.key_EnablePointerAlpha), false)) {
            if (alphaBarLayout != null) {
                Helper.showView(alphaBarLayout)
                Helper.showView(textAlpha)
            }
        } else {
            if (alphaBarLayout != null) {
                Helper.hideView(alphaBarLayout)
                Helper.hideView(textAlpha)
            }
        }
        selected_pointer.imageAlpha = alpha

        //pointer size
        seekBarSize.max = ((maxSize - minSize) / step)
        textSize.text = String.format(Locale.US, formatTextSize, getString(R.string.text_size), pointerSize, pointerSize)

        //pointer padding
        seekBarPadding.max = maxPadding
        seekBarPadding.progress = padding
        textPadding.text = String.format(Locale.US, formatPadding, getString(R.string.text_padding), padding)

        //pointer alpha
        textAlpha!!.text = String.format(Locale.US, formatPadding, getString(R.string.text_alpha), alpha)
        seekBarAlpha.progress = alpha

        setPointerImageParams(pointerSize, padding, true)
        setPointerSizeBarProgress(this, pointerSize)

        var currentSize: Int = seekBarSize.progress
        seekBarSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentSize = minSize + (progress * step)
                mEditor!!.putInt(getString(R.string.key_pointerSize), currentSize).apply()
                textSize.text = String.format(Locale.US, formatTextSize, getString(R.string.text_size), currentSize, currentSize)
                setPointerImageParams(currentSize, seekBarPadding.progress, false)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                setPointerImageParams(currentSize, seekBarPadding.progress, false)
            }

        })

        seekBarPadding.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            internal var imagePadding: Int = 0
            override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                mEditor!!.putInt(getString(R.string.key_pointerPadding), value).apply()
                textPadding.text = String.format(Locale.US, formatPadding, getString(R.string.text_padding), value)
                setPointerImageParams(currentSize, value, true)
                imagePadding = value
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                setPointerImageParams(seekBarSize.progress, imagePadding, true)
            }
        })

        seekBarAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                mEditor!!.putInt("pointerAlpha", value).apply()
                textAlpha.text = String.format(Locale.US, formatPadding, getString(R.string.text_alpha), value)
                selected_pointer.imageAlpha = value
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
    }

    private fun setPointerImageParams(size: Int, padding: Int, isApplyPadding: Boolean) {
        activity.image_customize_pointer.layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
        if (isApplyPadding) {
            activity.image_customize_pointer.setPadding(padding, padding, padding, padding)
        }
    }


    fun changeSeekVal(seek: View) {
        val progress = seekBarSize.progress
        val padding = seekBarPadding.progress

        when (seek.id) {
            R.id.butPlus -> setPointerSizeBarProgress(this, progress + 1)
            R.id.butMinus -> setPointerSizeBarProgress(this, progress - 1)
            R.id.butPaddingPlus -> seekBarPadding.progress = padding + 1
            R.id.butPaddingMinus -> seekBarPadding.progress = padding - 1
            R.id.butAlphaMinus -> seekBarAlpha.progress = seekBarAlpha.progress - 1
            R.id.butAlphaPlus -> seekBarAlpha.progress = seekBarAlpha.progress + 1
        }
    }

    companion object {
        fun newInstance(): CustomizeFragment {
            return CustomizeFragment()
        }

        fun setPointerSizeBarProgress(customizeFragment: CustomizeFragment, progress: Int) {
            customizeFragment.seekBarSize.progress = progress
        }
    }

}
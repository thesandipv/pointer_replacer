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
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.navigation.fragment.findNavController
import androidx.transition.ChangeBounds
import androidx.transition.ChangeTransform
import androidx.transition.TransitionSet
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.color.ColorPalette
import com.afollestad.materialdialogs.color.colorChooser
import com.afterroot.allusive.Constants.POINTER_TOUCH
import com.afterroot.allusive.GlideApp
import com.afterroot.allusive.R
import com.afterroot.allusive.utils.getDrawableExt
import com.afterroot.allusive.utils.getMinPointerSize
import com.afterroot.allusive.utils.getPrefs
import com.afterroot.allusive.utils.visible
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.fragment_customize_pointer.*
import kotlinx.android.synthetic.main.fragment_customize_pointer.view.*
import java.io.File
import java.util.*


/**
 * Created by Sandip on 04-10-2017.
 */
class CustomizeFragment : Fragment() {
    private lateinit var typeAlpha: String
    private lateinit var typeColor: String
    private lateinit var typePadding: String
    private lateinit var typePath: String
    private lateinit var typeSize: String
    private var mSharedPreferences: SharedPreferences? = null
    private var pointerType: Int = 0
    var color: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_customize_pointer, container, false)
        pointerType = arguments!!.getInt("TYPE")
        return view
    }

    @SuppressLint("CommitPrefEdits")
    override fun onStart() {
        super.onStart()

        mSharedPreferences = context!!.getPrefs()

        if (pointerType == POINTER_TOUCH) {
            typeColor = getString(R.string.key_pointerColor)
            typeSize = getString(R.string.key_pointerSize)
            typePadding = getString(R.string.key_pointerPadding)
            typeAlpha = getString(R.string.key_pointerAlpha)
            typePath = getString(R.string.key_selectedPointerPath)
            view!!.image_customize_pointer.transitionName = getString(R.string.main_fragment_transition)
        } else {
            typeColor = getString(R.string.key_mouseColor)
            typeSize = getString(R.string.key_mouseSize)
            typePadding = getString(R.string.key_mousePadding)
            typeAlpha = getString(R.string.key_mouseAlpha)
            typePath = getString(R.string.key_selectedMousePath)
            view!!.image_customize_pointer.transitionName = getString(R.string.transition_mouse)
        }

        TransitionSet()
            .addTransition(ChangeBounds())
            .addTransition(ChangeTransform())
            .apply {
                ordering = TransitionSet.ORDERING_TOGETHER
                duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
                interpolator = FastOutSlowInInterpolator()
                sharedElementEnterTransition = this
            }

        color = mSharedPreferences!!.getInt(typeColor, 0)
        view!!.image_customize_pointer.setColorFilter(color)

        GlideApp.with(context!!)
            .load(File(mSharedPreferences!!.getString(typePath, "")))
            .into(image_customize_pointer)

        activity!!.fab_apply.apply {
            setOnClickListener {
                mSharedPreferences!!.edit(true) {
                    putInt(typeSize, minSize + getView()!!.seekBarSize.progress)
                    putInt(typePadding, getView()!!.seekBarPadding.progress)
                    putInt(typeAlpha, getView()!!.seekBarAlpha.progress)
                    putInt(typeColor, color)
                }
                activity!!.fragment_repo_nav.findNavController().navigateUp()
            }
            icon = context!!.getDrawableExt(R.drawable.ic_action_apply)
        }

        setSeekBars()
        setClickListeners()
    }

    private val minSize: Int
        get() = context!!.getMinPointerSize()


    /**
     * Set initial values to SeekBar
     */
    private fun setSeekBars() {
        val maxSize = mSharedPreferences!!.getInt(getString(R.string.key_maxPointerSize), 100)
        val maxPadding = mSharedPreferences!!.getInt(getString(R.string.key_maxPaddingSize), 25)
        val alpha = mSharedPreferences!!.getInt(typeAlpha, 255)
        val pointerSize = mSharedPreferences!!.getInt(typeSize, minSize)
        val padding = mSharedPreferences!!.getInt(typePadding, 0)
        val formatTextSize = "%s: %d*%d "
        val formatPadding = "| %s: %d "

        with(mSharedPreferences!!.getBoolean(getString(R.string.key_EnablePointerAlpha), false)) {
            alphaBarLayout?.visible(this)
            textAlpha?.visible(this)
        }

        image_customize_pointer.imageAlpha = alpha

        //pointer size
        seekBarSize.max = maxSize - minSize
        seekBarSize.progress = pointerSize - minSize
        textSize.text = String.format(formatTextSize, getString(R.string.text_size), pointerSize, pointerSize)

        //pointer padding
        seekBarPadding.max = maxPadding
        seekBarPadding.progress = padding
        textPadding.text = String.format(formatPadding, getString(R.string.text_padding), padding)

        //pointer alpha
        textAlpha!!.text = String.format(formatPadding, getString(R.string.text_alpha), alpha)
        seekBarAlpha.progress = alpha

        var currentSize: Int = seekBarSize.progress + minSize
        setPointerImageParams(currentSize, padding)

        seekBarSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, newProgress: Int, fromUser: Boolean) {
                currentSize = minSize + newProgress
                //mSharedPreferences!!.edit(true) { putInt(getString(R.string.key_pointerSize), currentSize) }
                textSize.text =
                    String.format(Locale.US, formatTextSize, getString(R.string.text_size), currentSize, currentSize)
                setLayoutSize(currentSize)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                setPointerImageParams(currentSize, seekBarPadding.progress)
            }

        })

        seekBarPadding.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            var imagePadding: Int = 0
            override fun onProgressChanged(seekBar: SeekBar, newPadding: Int, fromUser: Boolean) {
                //mSharedPreferences!!.edit(true) { putInt(getString(R.string.key_pointerPadding), newPadding) }
                textPadding.text = String.format(Locale.US, formatPadding, getString(R.string.text_padding), newPadding)
                setLayoutPadding(newPadding)
                imagePadding = newPadding
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                setLayoutPadding(imagePadding)
            }
        })

        seekBarAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                //mSharedPreferences!!.edit(true) { putInt("pointerAlpha", value) }
                textAlpha.text = String.format(Locale.US, formatPadding, getString(R.string.text_alpha), value)
                image_customize_pointer.imageAlpha = value
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                image_customize_pointer.imageAlpha = seekBar.progress
            }
        })
    }

    private fun setPointerImageParams(size: Int, padding: Int) {
        setLayoutSize(size)
        setLayoutPadding(padding)
    }

    private fun setLayoutSize(size: Int) {
        activity!!.image_customize_pointer.layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)

    }

    private fun setLayoutPadding(padding: Int) {
        activity!!.image_customize_pointer.setPadding(padding, padding, padding, padding)

    }


    private fun setClickListeners() {
        butMinus.setOnClickListener {
            seekBarSize.progress = seekBarSize.progress - 1
        }

        butPlus.setOnClickListener {
            seekBarSize.progress = seekBarSize.progress + 1
        }

        butPaddingPlus.setOnClickListener {
            seekBarPadding.progress = seekBarPadding.progress + 1
        }

        butPaddingMinus.setOnClickListener {
            seekBarPadding.progress = seekBarPadding.progress - 1
        }

        butAlphaMinus.setOnClickListener {
            seekBarAlpha.progress = seekBarAlpha.progress - 1
        }

        butAlphaPlus.setOnClickListener {
            seekBarAlpha.progress = seekBarAlpha.progress + 1
        }

        val typeTmpColor = if (pointerType == POINTER_TOUCH) "POINTER_TMP_COLOR" else "MOUSE_TMP_COLOR"
        action_change_color.setOnClickListener {
            val tmpColor = mSharedPreferences!!.getInt(typeTmpColor, 0)

            MaterialDialog(context!!).show {
                title(R.string.choose_color)
                colorChooser(
                    ColorPalette.Primary,
                    ColorPalette.PrimarySub, allowCustomArgb = true, showAlphaSelector = true, initialSelection = tmpColor
                ) { _, selectedColor ->
                    this@CustomizeFragment.view!!.image_customize_pointer.setColorFilter(selectedColor)
                    color = selectedColor
                    mSharedPreferences!!.edit(true) {
                        putInt(typeTmpColor, selectedColor)
                    }
                }
                positiveButton(android.R.string.ok)
                negativeButton(android.R.string.cancel)
            }
        }

        action_reset_color.setOnClickListener {
            mSharedPreferences!!.edit(true) {
                putInt(typeTmpColor, 0)
            }
            this@CustomizeFragment.view!!.image_customize_pointer.setColorFilter(0)
        }
    }
}
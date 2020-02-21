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

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.navigation.findNavController
import androidx.transition.ChangeBounds
import androidx.transition.ChangeTransform
import androidx.transition.TransitionSet
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.color.ColorPalette
import com.afollestad.materialdialogs.color.colorChooser
import com.afterroot.allusive.Constants.POINTER_TOUCH
import com.afterroot.allusive.GlideApp
import com.afterroot.allusive.R
import com.afterroot.allusive.Settings
import com.afterroot.allusive.getMinPointerSize
import com.afterroot.core.extensions.getDrawableExt
import com.afterroot.core.extensions.visible
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.fragment_customize_pointer.*
import kotlinx.android.synthetic.main.fragment_customize_pointer.view.*
import org.koin.android.ext.android.inject
import java.io.File
import java.util.*


/**
 * Created by Sandip on 04-10-2017.
 */
class CustomizeFragment : Fragment() {
    private lateinit var typePath: String
    private val pointersDocument: DocumentFile by inject()
    private val settings: Settings by inject()
    private var pointerType: Int = 0
    private var selectedColor: Int = 0
    private var typeAlpha: Int = 255
    private var typeColor: Int = 0
    private var typePadding: Int = 0
    private var typeSize: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_customize_pointer, container, false)
        pointerType = arguments!!.getInt("TYPE")
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings.apply {
            if (pointerType == POINTER_TOUCH) {
                typeColor = pointerColor
                typeSize = pointerSize
                typePadding = pointerPadding
                typeAlpha = pointerAlpha
                typePath = selectedPointerPath!!
            } else {
                typeColor = mouseColor
                typeSize = mouseSize
                typePadding = mousePadding
                typeAlpha = mouseAlpha
                typePath = selectedMousePath!!
            }
        }

        view.image_customize_pointer.setColorFilter(typeColor)

        GlideApp.with(context!!)
            .load(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    pointersDocument.findFile(
                        if (pointerType == POINTER_TOUCH) settings.selectedPointerName!!
                        else settings.selectedMouseName!!
                    )
                        ?.uri
                } else {
                    Uri.fromFile(File(typePath))
                }
            )
            .into(image_customize_pointer)

        activity!!.fab_apply.apply {
            setOnClickListener {
                if (pointerType == POINTER_TOUCH) {
                    settings.apply {
                        pointerSize = minSize + getView()!!.seekBarSize.progress
                        pointerPadding = getView()!!.seekBarPadding.progress
                        pointerAlpha = getView()!!.seekBarAlpha.progress
                        pointerColor = selectedColor
                    }
                } else {
                    settings.apply {
                        mouseSize = minSize + getView()!!.seekBarSize.progress
                        mousePadding = getView()!!.seekBarPadding.progress
                        mouseAlpha = getView()!!.seekBarAlpha.progress
                        mouseColor = selectedColor
                    }
                }
                activity!!.fragment_repo_nav.findNavController().navigateUp()
            }
            icon = context!!.getDrawableExt(R.drawable.ic_action_apply)
        }

        setSeekBars()
        setClickListeners()
    }

    override fun onStart() {
        super.onStart()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (pointerType == POINTER_TOUCH) {
                view!!.image_customize_pointer.transitionName = getString(R.string.main_fragment_transition)
            } else {
                view!!.image_customize_pointer.transitionName = getString(R.string.transition_mouse)
            }
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
    }

    private val minSize: Int
        get() = context!!.getMinPointerSize()


    /**
     * Set initial values to SeekBar
     */
    private fun setSeekBars() {
        val maxSize = settings.maxPointerSize
        val maxPadding = settings.maxPointerPadding
        val alpha = typeAlpha
        val pointerSize = typeSize
        val padding = typePadding
        val formatTextSize = "%s: %d*%d "
        val formatPadding = "| %s: %d "

        with(settings.isEnableAlpha) {
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

        action_change_color.setOnClickListener {
            val tmpColor = if (pointerType == POINTER_TOUCH) settings.pointerTmpColor else settings.mouseTmpColor

            MaterialDialog(context!!).show {
                title(R.string.choose_color)
                colorChooser(
                    ColorPalette.Primary,
                    ColorPalette.PrimarySub, allowCustomArgb = true, showAlphaSelector = true, initialSelection = tmpColor
                ) { _, selectedColor ->
                    this@CustomizeFragment.view!!.image_customize_pointer.setColorFilter(selectedColor)
                    this@CustomizeFragment.selectedColor = selectedColor
                    if (pointerType == POINTER_TOUCH) {
                        settings.pointerTmpColor = selectedColor
                    } else settings.mouseTmpColor = selectedColor
                }
                positiveButton(android.R.string.ok)
                negativeButton(android.R.string.cancel)
            }
        }

        action_reset_color.setOnClickListener {
            if (pointerType == POINTER_TOUCH) {
                settings.pointerTmpColor = 0
            } else settings.mouseTmpColor = 0
            selectedColor = 0
            this@CustomizeFragment.view!!.image_customize_pointer.setColorFilter(0)
        }
    }
}
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

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.navigation.findNavController
import androidx.transition.ChangeBounds
import androidx.transition.ChangeTransform
import androidx.transition.TransitionSet
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.color.ColorPalette
import com.afollestad.materialdialogs.color.colorChooser
import com.afterroot.allusive2.Constants.POINTER_TOUCH
import com.afterroot.allusive2.R
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.databinding.FragmentCustomizePointerBinding
import com.afterroot.allusive2.getMinPointerSize
import com.afterroot.utils.extensions.getDrawableExt
import com.afterroot.utils.extensions.visible
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.Locale
import javax.inject.Inject
import org.jetbrains.anko.find
import com.afterroot.allusive2.resources.R as CommonR

/**
 * Created by Sandip on 04-10-2017.
 */
@AndroidEntryPoint
class CustomizeFragment : Fragment() {
  private lateinit var typePath: String

  @Inject lateinit var settings: Settings
  private var pointerType: Int = 0
  private var selectedColor: Int = 0
  private var typeAlpha: Int = 255
  private var typeColor: Int = 0
  private var typePadding: Int = 0
  private var typeSize: Int = 0
  private lateinit var binding: FragmentCustomizePointerBinding

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    binding = FragmentCustomizePointerBinding.inflate(inflater, container, false)
    pointerType = requireArguments().getInt("TYPE")
    return binding.root
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

    binding.imageCustomizePointer.setColorFilter(typeColor)

    Glide.with(requireContext())
      .load(Uri.fromFile(File(typePath)))
      .into(binding.imageCustomizePointer)

    requireActivity().find<ExtendedFloatingActionButton>(R.id.fab_apply).apply {
      setOnClickListener {
        if (pointerType == POINTER_TOUCH) {
          settings.apply {
            pointerSize = minSize + binding.seekBarSize.progress
            pointerPadding = binding.seekBarPadding.progress
            pointerAlpha = binding.seekBarAlpha.progress
            pointerColor = selectedColor
          }
        } else {
          settings.apply {
            mouseSize = minSize + binding.seekBarSize.progress
            mousePadding = binding.seekBarPadding.progress
            mouseAlpha = binding.seekBarAlpha.progress
            mouseColor = selectedColor
          }
        }
        requireActivity().find<FragmentContainerView>(
          R.id.fragment_repo_nav,
        ).findNavController().navigateUp()
      }
      icon = requireContext().getDrawableExt(CommonR.drawable.ic_action_apply)
      icon = requireContext().getDrawableExt(CommonR.drawable.ic_action_apply)
    }

    setSeekBars()
    setClickListeners()
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  override fun onStart() {
    super.onStart()

    if (pointerType == POINTER_TOUCH) {
      binding.imageCustomizePointer.transitionName =
        getString(CommonR.string.main_fragment_transition)
    } else {
      binding.imageCustomizePointer.transitionName =
        getString(CommonR.string.transition_mouse)
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
    get() = requireContext().getMinPointerSize()

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

    binding.apply {
      with(settings.isEnableAlpha) {
        alphaBarLayout.visible(this)
        textAlpha.visible(this)
      }

      imageCustomizePointer.imageAlpha = alpha

      // pointer size
      seekBarSize.max = maxSize - minSize
      seekBarSize.progress = pointerSize - minSize
      textSize.text =
        String.format(
          formatTextSize,
          getString(CommonR.string.text_size),
          pointerSize,
          pointerSize,
        )

      // pointer padding
      seekBarPadding.max = maxPadding
      seekBarPadding.progress = padding
      textPadding.text =
        String.format(formatPadding, getString(CommonR.string.text_padding), padding)

      // pointer alpha
      textAlpha.text =
        String.format(formatPadding, getString(CommonR.string.text_alpha), alpha)
      seekBarAlpha.progress = alpha

      var currentSize: Int = seekBarSize.progress + minSize
      setPointerImageParams(currentSize, padding)

      seekBarSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, newProgress: Int, fromUser: Boolean) {
          currentSize = minSize + newProgress
          textSize.text =
            String.format(
              Locale.US,
              formatTextSize,
              getString(CommonR.string.text_size),
              currentSize,
              currentSize,
            )
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
          textPadding.text =
            String.format(
              Locale.US,
              formatPadding,
              getString(CommonR.string.text_padding),
              newPadding,
            )
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
          textAlpha.text =
            String.format(
              Locale.US,
              formatPadding,
              getString(CommonR.string.text_alpha),
              value,
            )
          imageCustomizePointer.imageAlpha = value
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
          imageCustomizePointer.imageAlpha = seekBar.progress
        }
      })
    }
  }

  private fun setPointerImageParams(size: Int, padding: Int) {
    setLayoutSize(size)
    setLayoutPadding(padding)
  }

  private fun setLayoutSize(size: Int) {
    binding.imageCustomizePointer.layoutParams =
      FrameLayout.LayoutParams(size, size, Gravity.CENTER)
  }

  private fun setLayoutPadding(padding: Int) {
    binding.imageCustomizePointer.setPadding(padding, padding, padding, padding)
  }

  private fun setClickListeners() {
    binding.apply {
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

      actionChangeColor.setOnClickListener {
        val tmpColor = if (pointerType ==
          POINTER_TOUCH
        ) {
          settings.pointerTmpColor
        } else {
          settings.mouseTmpColor
        }

        MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
          title(CommonR.string.choose_color)
          @Suppress("UNUSED_VARIABLE")
          var dialog = colorChooser(
            ColorPalette.Primary,
            ColorPalette.PrimarySub,
            allowCustomArgb = true,
            showAlphaSelector = true,
            initialSelection = tmpColor,
          ) { _, selectedColor ->
            imageCustomizePointer.setColorFilter(selectedColor)
            this@CustomizeFragment.selectedColor = selectedColor
            if (pointerType == POINTER_TOUCH) {
              settings.pointerTmpColor = selectedColor
            } else {
              settings.mouseTmpColor = selectedColor
            }
          }
          positiveButton(android.R.string.ok)
          negativeButton(android.R.string.cancel)
        }
      }

      actionResetColor.setOnClickListener {
        if (pointerType == POINTER_TOUCH) {
          settings.pointerTmpColor = 0
        } else {
          settings.mouseTmpColor = 0
        }
        selectedColor = 0
        imageCustomizePointer.setColorFilter(0)
      }
    }
  }
}

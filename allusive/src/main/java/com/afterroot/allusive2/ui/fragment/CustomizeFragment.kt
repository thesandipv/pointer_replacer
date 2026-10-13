/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.color.ColorPalette
import com.afollestad.materialdialogs.color.colorChooser
import com.afterroot.allusive2.Constants.POINTER_TOUCH
import com.afterroot.allusive2.R
import com.afterroot.allusive2.Settings
import com.afterroot.allusive2.getMinPointerSize
import com.afterroot.ui.common.compose.theme.Theme
import com.afterroot.utils.extensions.getDrawableExt
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import com.afterroot.allusive2.resources.R as CommonR

/**
 * Created by Sandip on 04-10-2017.
 */
@AndroidEntryPoint
class CustomizeFragment : Fragment() {
  private lateinit var typePath: String

  @Inject lateinit var settings: Settings
  private var pointerType: Int = 0

  private val sizeState = MutableStateFlow(0)
  private val paddingState = MutableStateFlow(0)
  private val alphaState = MutableStateFlow(255)
  private val colorState = MutableStateFlow(0)

  private val minSize: Int
    get() = requireContext().getMinPointerSize()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pointerType = requireArguments().getInt("TYPE")
    initValues()
  }

  private fun initValues() {
    val min = minSize
    settings.apply {
      if (pointerType == POINTER_TOUCH) {
        sizeState.value = pointerSize.coerceAtLeast(min)
        paddingState.value = pointerPadding
        alphaState.value = pointerAlpha
        colorState.value = pointerColor
        typePath = selectedPointerPath.orEmpty()
      } else {
        sizeState.value = mouseSize.coerceAtLeast(min)
        paddingState.value = mousePadding
        alphaState.value = mouseAlpha
        colorState.value = mouseColor
        typePath = selectedMousePath.orEmpty()
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View = ComposeView(requireContext()).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
      Theme {
        val size by sizeState.collectAsStateWithLifecycle()
        val padding by paddingState.collectAsStateWithLifecycle()
        val alpha by alphaState.collectAsStateWithLifecycle()
        val color by colorState.collectAsStateWithLifecycle()

        CustomizePointerScreen(
          typePath = typePath,
          size = size,
          minSize = minSize,
          maxSize = settings.maxPointerSize,
          padding = padding,
          maxPadding = settings.maxPointerPadding,
          alpha = alpha,
          isAlphaEnabled = settings.isEnableAlpha,
          tintColor = color,
          onSizeChange = { sizeState.value = it },
          onPaddingChange = { paddingState.value = it },
          onAlphaChange = { alphaState.value = it },
          onChangeColorClick = { showColorChooser() },
          onResetColorClick = { resetColor() },
        )
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    requireActivity().findViewById<ExtendedFloatingActionButton>(R.id.fab_apply)?.apply {
      icon = requireContext().getDrawableExt(CommonR.drawable.ic_action_apply)
      setOnClickListener {
        val sizeVal = sizeState.value
        val paddingVal = paddingState.value
        val alphaVal = alphaState.value
        val colorVal = colorState.value

        if (pointerType == POINTER_TOUCH) {
          settings.apply {
            pointerSize = sizeVal
            pointerPadding = paddingVal
            pointerAlpha = alphaVal
            pointerColor = colorVal
          }
        } else {
          settings.apply {
            mouseSize = sizeVal
            mousePadding = paddingVal
            mouseAlpha = alphaVal
            mouseColor = colorVal
          }
        }
        findNavController().navigateUp()
      }
    }
  }

  @SuppressLint("CheckResult")
  private fun showColorChooser() {
    val tmpColor = if (pointerType == POINTER_TOUCH) {
      settings.pointerTmpColor
    } else {
      settings.mouseTmpColor
    }

    MaterialDialog(requireContext()).show {
      title(CommonR.string.choose_color)
      colorChooser(
        ColorPalette.Primary,
        ColorPalette.PrimarySub,
        allowCustomArgb = true,
        showAlphaSelector = true,
        initialSelection = tmpColor,
      ) { _, selectedColor ->
        colorState.value = selectedColor
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

  private fun resetColor() {
    if (pointerType == POINTER_TOUCH) {
      settings.pointerTmpColor = 0
    } else {
      settings.mouseTmpColor = 0
    }
    colorState.value = 0
  }
}

@Composable
fun CustomizePointerScreen(
  typePath: String,
  size: Int,
  minSize: Int,
  maxSize: Int,
  padding: Int,
  maxPadding: Int,
  alpha: Int,
  isAlphaEnabled: Boolean,
  tintColor: Int,
  onSizeChange: (Int) -> Unit,
  onPaddingChange: (Int) -> Unit,
  onAlphaChange: (Int) -> Unit,
  onChangeColorClick: () -> Unit,
  onResetColorClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Card(
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = stringResource(CommonR.string.pointer_preview),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )

        Box(
          modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center,
        ) {
          val imageBitmap = ImageBitmap.imageResource(id = CommonR.drawable.transparent_rect)

          // Remember the brush so it doesn't get re-allocated on every recomposition
          val tiledBrush = remember(imageBitmap) {
            ShaderBrush(
              ImageShader(
                image = imageBitmap,
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Repeated,
              ),
            )
          }
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(tiledBrush),
          )

          if (typePath.isNotEmpty()) {
            val context = LocalContext.current
            val painter = rememberAsyncImagePainter(
              model = ImageRequest.Builder(context)
                .data(File(typePath))
                .crossfade(true)
                .build(),
            )

            val density = LocalDensity.current
            val sizeDp = with(density) { size.toDp() }
            val paddingDp = with(density) { padding.toDp() }

            Image(
              painter = painter,
              contentDescription = null,
              colorFilter = if (tintColor != 0) ColorFilter.tint(Color(tintColor)) else null,
              alpha = alpha / 255f,
              modifier = Modifier
                .size(sizeDp)
                .padding(paddingDp),
            )
          }
        }
      }
    }

    Card(
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "${stringResource(CommonR.string.text_size)}: $size*$size",
            style = MaterialTheme.typography.bodyMedium,
          )
          Text(
            text = "| ${stringResource(CommonR.string.text_padding)}: $padding",
            style = MaterialTheme.typography.bodyMedium,
          )
          if (isAlphaEnabled) {
            Text(
              text = "| ${stringResource(CommonR.string.text_alpha)}: $alpha",
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }

        SliderControlRow(
          value = size,
          valueRange = minSize.toFloat()..maxSize.toFloat(),
          onValueChange = onSizeChange,
          contentDescriptionMinus = "Decrease size",
          contentDescriptionPlus = "Increase size",
        )

        SliderControlRow(
          value = padding,
          valueRange = 0f..maxPadding.toFloat(),
          onValueChange = onPaddingChange,
          contentDescriptionMinus = "Decrease padding",
          contentDescriptionPlus = "Increase padding",
        )

        if (isAlphaEnabled) {
          SliderControlRow(
            value = alpha,
            valueRange = 0f..255f,
            onValueChange = onAlphaChange,
            contentDescriptionMinus = "Decrease alpha",
            contentDescriptionPlus = "Increase alpha",
          )
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
      OutlinedButton(
        onClick = onResetColorClick,
      ) {
        Text(text = stringResource(CommonR.string.reset_color))
      }
      Button(
        onClick = onChangeColorClick,
      ) {
        Text(text = stringResource(CommonR.string.change_color))
      }
    }
  }
}

@Composable
private fun SliderControlRow(
  value: Int,
  valueRange: ClosedFloatingPointRange<Float>,
  onValueChange: (Int) -> Unit,
  contentDescriptionMinus: String,
  contentDescriptionPlus: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    IconButton(
      onClick = {
        onValueChange((value - 1).coerceAtLeast(valueRange.start.toInt()))
      },
    ) {
      Icon(
        painter = painterResource(CommonR.drawable.ic_action_minus),
        contentDescription = contentDescriptionMinus,
      )
    }

    Slider(
      value = value.toFloat(),
      onValueChange = { onValueChange(it.roundToInt()) },
      valueRange = valueRange,
      modifier = Modifier.weight(1f),
    )

    IconButton(
      onClick = {
        onValueChange((value + 1).coerceAtMost(valueRange.endInclusive.toInt()))
      },
    ) {
      Icon(
        painter = painterResource(CommonR.drawable.ic_action_plus),
        contentDescription = contentDescriptionPlus,
      )
    }
  }
}

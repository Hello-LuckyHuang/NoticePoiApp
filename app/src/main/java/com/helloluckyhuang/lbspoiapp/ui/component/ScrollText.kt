package com.helloluckyhuang.lbspoiapp.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ScrollText(
    text: String,
    maxChars: Int,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textDecoration: TextDecoration? = null,
    style: TextStyle = LocalTextStyle.current,
    gap: Dp = 32.dp,
    speed: Dp = 40.dp,
) {
    BoxWithConstraints(
        modifier = modifier.clipToBounds()
    ) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()

        val gapPx = with(density) { gap.toPx() }
        val speedPxPerSecond = with(density) { speed.toPx() }

        val finalStyle = style.copy(
            color = color,
            textDecoration = textDecoration
        )

        val textLayoutResult = remember(text, finalStyle, textMeasurer) {
            textMeasurer.measure(
                text = AnnotatedString(text),
                style = finalStyle,
                maxLines = 1,
                softWrap = false
            )
        }

        val textWidthPx = textLayoutResult.size.width.toFloat()
        val shouldScroll = text.length > maxChars && textWidthPx > 0f

        if (!shouldScroll) {
            Text(
                text = text,
                maxLines = 1,
                softWrap = false,
                color = color,
                textDecoration = textDecoration,
                style = style
            )
        } else {
            val travelDistance = (textWidthPx + gapPx).coerceAtLeast(1f)

            val infiniteTransition = rememberInfiniteTransition(label = "scroll_text")

            val offsetX by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -travelDistance,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = ((travelDistance / speedPxPerSecond) * 1000)
                            .roundToInt()
                            .coerceAtLeast(1),
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "scroll_offset"
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = text,
                    maxLines = 1,
                    softWrap = false,
                    color = color,
                    textDecoration = textDecoration,
                    style = style,
                    modifier = Modifier
                        .wrapContentWidth(unbounded = true)
                        .offset { IntOffset(offsetX.roundToInt(), 0) }
                )

                Text(
                    text = text,
                    maxLines = 1,
                    softWrap = false,
                    color = color,
                    textDecoration = textDecoration,
                    style = style,
                    modifier = Modifier
                        .wrapContentWidth(unbounded = true)
                        .offset {
                            IntOffset((offsetX + textWidthPx + gapPx).roundToInt(), 0)
                        }
                )
            }
        }
    }
}
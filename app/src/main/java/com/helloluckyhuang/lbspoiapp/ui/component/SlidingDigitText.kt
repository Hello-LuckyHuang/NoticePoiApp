package com.helloluckyhuang.lbspoiapp.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnit.Companion

private enum class CharScrollDirection {
    Up,
    Down,
    None
}

/**
 * 滑动数字文本组件
 *
 * @param text 目标文本
 * @param modifier 修饰符
 * @param style 文本样式。建议使用等宽数字，避免宽度跳动
 */
@Composable
fun SlidingDigitText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default.copy(
        fontFeatureSettings = "\"tnum\"" // tabular numbers，等宽数字
    ),
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    color: Color = Color.Black
) {
    Row(
        modifier = modifier.wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        text.forEachIndexed { index, targetChar ->
            key(index) {
                AnimatedContent(
                    targetState = targetChar,
                    transitionSpec = {
                        createCharTransform(
                            from = initialState,
                            to = targetState
                        )
                    },
                    label = "SlidingDigitChar-$index"
                ) { animatedChar ->
                    Text(
                        text = animatedChar.toString(),
                        style = style,
                        color = color,
                        fontWeight = fontWeight,
                        fontFamily = fontFamily,
                        fontSize = fontSize
                    )
                }
            }
        }
    }
}

private fun createCharTransform(
    from: Char,
    to: Char
): ContentTransform {
    val direction = resolveDirection(from, to)

    val duration = 220

    return when (direction) {
        CharScrollDirection.Up -> {
            (
                    slideInVertically(
                        animationSpec = tween(duration, easing = FastOutSlowInEasing),
                        initialOffsetY = { height -> height } // 新字符从下方进入
                    ) +
                            fadeIn(animationSpec = tween(duration)) +
                            scaleIn(
                                animationSpec = tween(duration),
                                initialScale = 0.92f
                            )
                    ).togetherWith(
                    slideOutVertically(
                        animationSpec = tween(duration, easing = FastOutSlowInEasing),
                        targetOffsetY = { height -> -height } // 旧字符向上离开
                    ) +
                            fadeOut(animationSpec = tween(duration)) +
                            scaleOut(
                                animationSpec = tween(duration),
                                targetScale = 1.08f
                            )
                )
        }

        CharScrollDirection.Down -> {
            (
                    slideInVertically(
                        animationSpec = tween(duration, easing = FastOutSlowInEasing),
                        initialOffsetY = { height -> -height } // 新字符从上方进入
                    ) +
                            fadeIn(animationSpec = tween(duration)) +
                            scaleIn(
                                animationSpec = tween(duration),
                                initialScale = 1.08f
                            )
                    ).togetherWith(
                    slideOutVertically(
                        animationSpec = tween(duration, easing = FastOutSlowInEasing),
                        targetOffsetY = { height -> height } // 旧字符向下离开
                    ) +
                            fadeOut(animationSpec = tween(duration)) +
                            scaleOut(
                                animationSpec = tween(duration),
                                targetScale = 0.92f
                            )
                )
        }

        CharScrollDirection.None -> {
            (
                    fadeIn(animationSpec = tween(90))
                    ).togetherWith(
                    fadeOut(animationSpec = tween(90))
                )
        }
    }
}

/**
 * 数字规则：
 * - 0~9 比较大小
 * - 9 -> 0 视为变大（向上）
 * - 0 -> 9 视为变小（向下）
 *
 * 非数字规则：
 * - 只要变化，固定向下
 */
private fun resolveDirection(from: Char, to: Char): CharScrollDirection {
    if (from == to) return CharScrollDirection.None

    val fromDigit = from.digitToIntOrNull()
    val toDigit = to.digitToIntOrNull()

    return if (fromDigit != null && toDigit != null) {
        when {
            fromDigit == 9 && toDigit == 0 -> CharScrollDirection.Up
            fromDigit == 0 && toDigit == 9 -> CharScrollDirection.Down
            toDigit > fromDigit -> CharScrollDirection.Up
            toDigit < fromDigit -> CharScrollDirection.Down
            else -> CharScrollDirection.None
        }
    } else {
        // 非数字：固定向下滑动
        CharScrollDirection.Down
    }
}
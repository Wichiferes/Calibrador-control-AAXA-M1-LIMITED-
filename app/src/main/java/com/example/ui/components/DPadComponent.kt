package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun DPadComponent(
    modifier: Modifier = Modifier,
    onUpPressed: () -> Unit,
    onDownPressed: () -> Unit,
    onLeftPressed: () -> Unit,
    onRightPressed: () -> Unit,
    onOkPressed: () -> Unit,
    onPressStart: (zone: Int) -> Unit, // 0: up, 1: right, 2: down, 3: left
    onPressEnd: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .size(180.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val dist = hypot(dx, dy)
                        val centerRadius = 50.dp.toPx()
                        
                        var pressedZone = -1
                        if (dist > centerRadius) {
                            val angleRad = atan2(dy, dx)
                            val angleDeg = Math.toDegrees(angleRad.toDouble())
                            pressedZone = when {
                                angleDeg in -135.0..-45.0 -> 0 // Up
                                angleDeg > -45.0 && angleDeg <= 45.0 -> 1 // Right
                                angleDeg > 45.0 && angleDeg <= 135.0 -> 2 // Down
                                else -> 3 // Left
                            }
                            onPressStart(pressedZone)
                        }
                        
                        tryAwaitRelease()
                        onPressEnd()
                    },
                    onTap = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val dist = hypot(dx, dy)
                        val centerRadius = 50.dp.toPx()

                        if (dist <= centerRadius) {
                            onOkPressed()
                        } else {
                            val angleRad = atan2(dy, dx)
                            val angleDeg = Math.toDegrees(angleRad.toDouble())
                            when {
                                angleDeg in -135.0..-45.0 -> onUpPressed()
                                angleDeg > -45.0 && angleDeg <= 45.0 -> onRightPressed()
                                angleDeg > 45.0 && angleDeg <= 135.0 -> onDownPressed()
                                else -> onLeftPressed()
                            }
                        }
                    }
                )
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.width / 2f
        val innerRadius = 50.dp.toPx()

        drawCircle(
            color = Color(0xFF1E1E3F),
            radius = outerRadius,
            center = center
        )
        drawCircle(
            color = Color(0xFF7C3AED),
            radius = outerRadius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
        
        drawCircle(
            color = Color(0xFF1E1E3F),
            radius = innerRadius,
            center = center
        )
        drawCircle(
            color = Color(0xFF7C3AED),
            radius = innerRadius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        
        // Boundaries
        val boundaryLength = outerRadius - innerRadius
        for (i in 0..3) {
            val angle = Math.toRadians((i * 90 - 45).toDouble())
            val startX = center.x + innerRadius * kotlin.math.cos(angle).toFloat()
            val startY = center.y + innerRadius * kotlin.math.sin(angle).toFloat()
            val endX = center.x + outerRadius * kotlin.math.cos(angle).toFloat()
            val endY = center.y + outerRadius * kotlin.math.sin(angle).toFloat()
            drawLine(
                color = Color(0xFF7C3AED),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw labels
        val textStyleOuter = TextStyle(color = Color.White, fontSize = 24.sp)
        val measureUp = textMeasurer.measure("▲", textStyleOuter)
        drawText(
            textMeasurer = textMeasurer,
            text = "▲",
            style = textStyleOuter,
            topLeft = Offset(center.x - measureUp.size.width / 2, center.y - outerRadius + 12.dp.toPx())
        )
        val measureDown = textMeasurer.measure("▼", textStyleOuter)
        drawText(
            textMeasurer = textMeasurer,
            text = "▼",
            style = textStyleOuter,
            topLeft = Offset(center.x - measureDown.size.width / 2, center.y + outerRadius - measureDown.size.height - 12.dp.toPx())
        )
        val measureLeft = textMeasurer.measure("◀", textStyleOuter)
        drawText(
            textMeasurer = textMeasurer,
            text = "◀",
            style = textStyleOuter,
            topLeft = Offset(center.x - outerRadius + 12.dp.toPx(), center.y - measureLeft.size.height / 2)
        )
        val measureRight = textMeasurer.measure("▶", textStyleOuter)
        drawText(
            textMeasurer = textMeasurer,
            text = "▶",
            style = textStyleOuter,
            topLeft = Offset(center.x + outerRadius - measureRight.size.width - 12.dp.toPx(), center.y - measureRight.size.height / 2)
        )
        
        val textStyleOk = TextStyle(color = Color(0xFF06B6D4), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        val measureOk = textMeasurer.measure("OK", textStyleOk)
        drawText(
            textMeasurer = textMeasurer,
            text = "OK",
            style = textStyleOk,
            topLeft = Offset(center.x - measureOk.size.width / 2, center.y - measureOk.size.height / 2)
        )
    }
}

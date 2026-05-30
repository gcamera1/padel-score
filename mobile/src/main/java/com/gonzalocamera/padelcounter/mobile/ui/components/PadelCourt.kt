package com.gonzalocamera.padelcounter.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelTheme

@Composable
fun PadelCourt(
    courtColor: Color,
    modifier: Modifier = Modifier,
) {
    val lineColor = PadelTheme.colors.courtLine
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val landscape = w > h

        drawRect(color = courtColor.copy(alpha = 0.28f))

        val thick = 4f
        val thin = 2f

        // Outer border
        drawLine(lineColor, Offset(0f, 0f), Offset(w, 0f), strokeWidth = thick)
        drawLine(lineColor, Offset(0f, h), Offset(w, h), strokeWidth = thick)
        drawLine(lineColor, Offset(0f, 0f), Offset(0f, h), strokeWidth = thick)
        drawLine(lineColor, Offset(w, 0f), Offset(w, h), strokeWidth = thick)

        if (landscape) {
            val netX = w / 2f
            drawLine(lineColor, Offset(netX, 0f), Offset(netX, h), strokeWidth = thick)

            val half = w / 2f
            val serviceFromNet = half * (7f / 10f)
            val serviceLeftX = netX - serviceFromNet
            val serviceRightX = netX + serviceFromNet

            drawLine(lineColor, Offset(serviceLeftX, h / 2f), Offset(serviceRightX, h / 2f), strokeWidth = thin)
            drawLine(lineColor, Offset(serviceLeftX, 0f), Offset(serviceLeftX, h), strokeWidth = thin)
            drawLine(lineColor, Offset(serviceRightX, 0f), Offset(serviceRightX, h), strokeWidth = thin)
        } else {
            val netY = h / 2f
            drawLine(lineColor, Offset(0f, netY), Offset(w, netY), strokeWidth = thick)

            val half = h / 2f
            val serviceFromNet = half * (7f / 10f)
            val serviceTopY = netY - serviceFromNet
            val serviceBottomY = netY + serviceFromNet

            drawLine(lineColor, Offset(w / 2f, serviceTopY), Offset(w / 2f, serviceBottomY), strokeWidth = thin)
            drawLine(lineColor, Offset(0f, serviceTopY), Offset(w, serviceTopY), strokeWidth = thin)
            drawLine(lineColor, Offset(0f, serviceBottomY), Offset(w, serviceBottomY), strokeWidth = thin)
        }
    }
}

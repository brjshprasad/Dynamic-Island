package com.custom.galaxylauncher.dynamicisland

import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.view.WindowInsets
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class CutoutPosition(
    val topMarginDp: Dp,
    val centerOffsetXDp: Dp,
    val cutoutWidthDp: Dp,
    val cutoutHeightDp: Dp
)

object CutoutGeometryHelper {

    fun getCutoutPosition(activity: Activity, userOffsetX: Int = 0, userOffsetY: Int = 0): CutoutPosition {
        var topMargin = 8.dp
        var cutoutWidth = 110.dp
        var cutoutHeight = 34.dp

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val windowInsets = activity.window.decorView.rootWindowInsets
            val cutout = windowInsets?.displayCutout

            if (cutout != null) {
                val topRect = cutout.boundingRectTop
                if (!topRect.isEmpty) {
                    val density = activity.resources.displayMetrics.density
                    val topDp = (topRect.top / density).dp
                    val heightDp = ((topRect.bottom - topRect.top) / density).dp

                    // Position pill centered vertically over the cutout
                    topMargin = if (topDp > 0.dp) topDp else 8.dp
                    if (heightDp > 20.dp) {
                        cutoutHeight = heightDp + 8.dp
                    }
                }
            } else {
                // Galaxy F62 standard status bar camera level fallback
                topMargin = 10.dp
            }
        }

        // Apply fine-tuning user calibration offsets
        return CutoutPosition(
            topMarginDp = topMargin + userOffsetY.dp,
            centerOffsetXDp = userOffsetX.dp,
            cutoutWidthDp = cutoutWidth,
            cutoutHeightDp = cutoutHeight
        )
    }
}

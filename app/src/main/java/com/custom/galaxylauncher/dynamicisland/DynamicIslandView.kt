package com.custom.galaxylauncher.dynamicisland

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.custom.galaxylauncher.model.DynamicIslandState

@Composable
fun DynamicIslandView(
    topMargin: Dp,
    horizontalOffset: Dp,
    baseWidth: Dp,
    baseHeight: Dp,
    onToggleTorch: () -> Unit,
    onLaunchCamera: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val islandState by DynamicIslandController.currentState.collectAsState()

    // Dynamic width and height based on active state
    val targetWidth = when (islandState) {
        is DynamicIslandState.Idle -> baseWidth.coerceAtLeast(115.dp)
        is DynamicIslandState.Flashlight -> 220.dp
        is DynamicIslandState.CameraLaunched -> 220.dp
        is DynamicIslandState.BatteryCharging -> 210.dp
        is DynamicIslandState.RingerMode -> 200.dp
        is DynamicIslandState.Media -> 250.dp
        is DynamicIslandState.Expanded -> 290.dp
    }

    val targetHeight = when (islandState) {
        is DynamicIslandState.Idle -> baseHeight.coerceAtLeast(36.dp)
        is DynamicIslandState.Expanded -> 88.dp
        else -> 44.dp
    }

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "islandWidth"
    )

    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "islandHeight"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topMargin),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .offset(x = horizontalOffset)
                .width(animatedWidth)
                .height(animatedHeight)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(36.dp), spotColor = Color.Black)
                .clip(RoundedCornerShape(36.dp))
                .background(Color.Black)
                .border(0.8.dp, Color(0x33FFFFFF), RoundedCornerShape(36.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    vibrateFeedback(context)
                    when (islandState) {
                        is DynamicIslandState.Idle -> {
                            DynamicIslandController.showState(
                                DynamicIslandState.Expanded(
                                    title = "Galaxy Island",
                                    subtitle = "Quick Controls"
                                ),
                                autoDismissMs = 4500L
                            )
                        }
                        is DynamicIslandState.Flashlight -> onToggleTorch()
                        is DynamicIslandState.CameraLaunched -> onLaunchCamera()
                        is DynamicIslandState.Expanded -> DynamicIslandController.dismiss()
                        else -> DynamicIslandController.dismiss()
                    }
                }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = islandState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "islandContent"
            ) { state ->
                when (state) {
                    is DynamicIslandState.Idle -> {
                        // Resting state framing Galaxy F62 center camera punch hole
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        ) {
                            // Left dot indicator
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34C759))
                            )

                            // Right dot indicator (camera lens framing aura)
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2C2C2E))
                            )
                        }
                    }
                    is DynamicIslandState.Flashlight -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (state.isOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                contentDescription = "Flashlight",
                                tint = if (state.isOn) Color(0xFFFFD60A) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (state.isOn) "Torch On" else "Torch Off",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    is DynamicIslandState.CameraLaunched -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera",
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.message,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    is DynamicIslandState.BatteryCharging -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = "Charging",
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${state.level}% Charging",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    is DynamicIslandState.RingerMode -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Ringer",
                                tint = Color(0xFFFF9500),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.mode,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    is DynamicIslandState.Media -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Music",
                                tint = Color(0xFFFF2D55),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${state.title} • ${state.artist}",
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                    is DynamicIslandState.Expanded -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Torch toggle button
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2C2C2E))
                                        .clickable {
                                            vibrateFeedback(context)
                                            onToggleTorch()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlashlightOn,
                                        contentDescription = "Torch",
                                        tint = Color(0xFFFFD60A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Camera button
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2C2C2E))
                                        .clickable {
                                            vibrateFeedback(context)
                                            onLaunchCamera()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Camera",
                                        tint = Color(0xFF007AFF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Settings button
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2C2C2E))
                                        .clickable {
                                            vibrateFeedback(context)
                                            onOpenSettings()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Chop: Torch • Twist: Camera",
                                color = Color(0xFF8E8E93),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun vibrateFeedback(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                v.vibrate(30L)
            }
        }
    } catch (_: Exception) {}
}

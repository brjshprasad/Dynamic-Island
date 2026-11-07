package com.custom.dynamicisland.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.custom.dynamicisland.MainActivity
import com.custom.dynamicisland.model.IslandSettings
import com.custom.dynamicisland.model.IslandState
import com.custom.dynamicisland.model.ManeuverType
import com.custom.dynamicisland.service.IslandController
import com.custom.dynamicisland.service.IslandNotificationListener
import kotlin.math.abs

@Composable
fun DynamicIslandOverlayView(
    settings: IslandSettings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val islandState by IslandController.currentState.collectAsState()
    val activeItems by IslandController.activeItems.collectAsState()
    val currentIndex by IslandController.currentIndex.collectAsState()

    // Enlargement is active for user-expanded media, quick controls, or turn/flyover/bifurcation warnings
    val isExpanded = when (islandState) {
        is IslandState.Media -> (islandState as IslandState.Media).isExpanded
        is IslandState.Navigation -> (islandState as IslandState.Navigation).isEnlarged
        is IslandState.QuickControls -> true
        else -> false
    }

    // Dynamic width and height: Navigation enlarges at turns and shrinks after 4s
    val isIdleAndHidden = (islandState is IslandState.Idle) && settings.hideWhenIdle

    val targetWidth: Dp = when (islandState) {
        is IslandState.Idle -> if (settings.hideWhenIdle) 0.dp else settings.baseWidth.dp
        is IslandState.Media -> if (isExpanded) 340.dp else 225.dp
        is IslandState.Navigation -> if (isExpanded) 340.dp else 225.dp
        is IslandState.Notification -> 245.dp  // Compact pill: does not obscure screen
        is IslandState.Call -> 235.dp          // Compact call pill
        is IslandState.Battery -> 210.dp
        is IslandState.Ringer -> 200.dp
        is IslandState.Bluetooth -> 230.dp
        is IslandState.QuickControls -> 300.dp
    }

    val targetHeight: Dp = when (islandState) {
        is IslandState.Idle -> if (settings.hideWhenIdle) 0.dp else settings.baseHeight.dp
        is IslandState.Media -> if (isExpanded) 136.dp else settings.baseHeight.dp.coerceAtLeast(34.dp)
        is IslandState.Navigation -> if (isExpanded) 112.dp else settings.baseHeight.dp.coerceAtLeast(34.dp)
        is IslandState.Notification -> settings.baseHeight.dp.coerceAtLeast(36.dp)
        is IslandState.Call -> settings.baseHeight.dp.coerceAtLeast(36.dp)
        is IslandState.QuickControls -> 86.dp
        else -> 38.dp
    }

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 450f),
        label = "islandWidth"
    )

    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 450f),
        label = "islandHeight"
    )

    // Translucent background so the underlying screen (caller name, wallpaper, etc.) is visible
    val islandBgColor = if (isIdleAndHidden) {
        Color.Transparent
    } else if (isExpanded) {
        Color.Black.copy(alpha = settings.backgroundOpacity.coerceIn(0.40f, 0.95f))
    } else {
        Color.Black.copy(alpha = settings.backgroundOpacity.coerceAtLeast(0.85f))
    }

    val islandBorderColor = if (isIdleAndHidden) {
        Color.Transparent
    } else if (isExpanded) {
        Color(0x55FFFFFF)
    } else {
        Color(0x33FFFFFF)
    }

    Box(
        modifier = modifier
            .width(animatedWidth)
            .height(animatedHeight)
            .shadow(
                elevation = if (isIdleAndHidden) 0.dp else if (isExpanded) 16.dp else 10.dp,
                shape = RoundedCornerShape(settings.cornerRadius.dp),
                spotColor = Color(0x88000000)
            )
            .clip(RoundedCornerShape(settings.cornerRadius.dp))
            .background(islandBgColor)
            .border(
                width = if (isIdleAndHidden) 0.dp else 0.8.dp,
                color = islandBorderColor,
                shape = RoundedCornerShape(settings.cornerRadius.dp)
            )
            .pointerInput(isExpanded) {
                var totalDragX = 0f
                var totalDragY = 0f
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                    },
                    onDragEnd = {
                        if (abs(totalDragX) > abs(totalDragY)) {
                            // "scroll to change function on expanded dynamic island only"
                            if (isExpanded) {
                                if (totalDragX < -25f) {
                                    vibrate(context, settings.enableHaptics)
                                    IslandController.nextItem()
                                } else if (totalDragX > 25f) {
                                    vibrate(context, settings.enableHaptics)
                                    IslandController.previousItem()
                                }
                            }
                        } else {
                            // VERTICAL SWIPE UP: collapse or dismiss
                            if (totalDragY < -25f) {
                                vibrate(context, settings.enableHaptics)
                                IslandController.dismissCurrentItem()
                            }
                        }
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                vibrate(context, settings.enableHaptics)
                if (!isExpanded) {
                    // "the dynamic island should expand on touch"
                    when (val state = islandState) {
                        is IslandState.Media -> {
                            IslandController.expandMedia()
                        }
                        is IslandState.Navigation -> {
                            IslandController.expandNavigation()
                        }
                        is IslandState.Notification -> {
                            try { state.contentIntent?.send() } catch (_: Exception) {}
                            IslandController.showState(IslandState.QuickControls(), autoDismissMs = 4500L)
                        }
                        is IslandState.Call -> {
                            val intent = Intent(Intent.ACTION_DIAL).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                        is IslandState.Idle -> {
                            IslandController.showState(IslandState.QuickControls(), autoDismissMs = 4500L)
                        }
                        else -> {
                            IslandController.showState(IslandState.QuickControls(), autoDismissMs = 4500L)
                        }
                    }
                } else {
                    // Already expanded: tap action
                    when (val state = islandState) {
                        is IslandState.Media -> {
                            IslandController.toggleMediaExpansion()
                        }
                        is IslandState.Navigation -> {
                            try { state.contentIntent?.send() } catch (_: Exception) {}
                            IslandController.shrinkNavigation()
                        }
                        is IslandState.Notification -> {
                            try { state.contentIntent?.send() } catch (_: Exception) {}
                            IslandController.dismissCurrentItem()
                        }
                        is IslandState.Call -> {
                            val intent = Intent(Intent.ACTION_DIAL).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                        is IslandState.QuickControls -> {
                            IslandController.dismiss()
                        }
                        else -> {
                            IslandController.dismissCurrentItem()
                        }
                    }
                }
            }
            .padding(horizontal = if (isIdleAndHidden) 0.dp else 12.dp, vertical = if (isIdleAndHidden) 0.dp else 4.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = islandState,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "islandContent"
        ) { state ->
            when (state) {
                is IslandState.Idle -> {
                    if (settings.hideWhenIdle) {
                        Spacer(modifier = Modifier.size(0.dp))
                    } else {
                        // Resting state framing punch-hole camera
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34C759))
                            )
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2C2C2E))
                            )
                        }
                    }
                }

                is IslandState.Media -> {
                    if (state.isExpanded) {
                        // Full Translucent Apple-style Dynamic Island Music Player Card
                        ExpandedMediaView(state = state, context = context)
                    } else {
                        // Compact Media Pill: Music note on left, live animated equalizer on right
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color(0xFFFF2D55),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = state.title,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Multi-app indicator dot if multiple apps running
                                if (activeItems.size > 1) {
                                    MultiAppIndicatorDots(count = activeItems.size, currentIndex = currentIndex)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                EqualizerBars(isPlaying = state.isPlaying)
                            }
                        }
                    }
                }

                is IslandState.Notification -> {
                    // Non-intrusive compact pill for messages (prevents obscuring caller name/main screen)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF007AFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${state.title}: ${state.message}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (activeItems.size > 1) {
                            MultiAppIndicatorDots(count = activeItems.size, currentIndex = currentIndex)
                        }
                    }
                }

                is IslandState.Call -> {
                    // Non-intrusive compact pill for calls (never obscures native caller name or answer UI)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34C759)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Call: ${state.callerName}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (activeItems.size > 1) {
                            MultiAppIndicatorDots(count = activeItems.size, currentIndex = currentIndex)
                        }
                    }
                }

                is IslandState.Navigation -> {
                    // Turn-by-Turn navigation: Enlarges at turns, flyovers, bifurcations; shrinks after 4s
                    if (state.isEnlarged) {
                        ExpandedNavigationView(
                            state = state,
                            activeItemsCount = activeItems.size,
                            currentIndex = currentIndex
                        )
                    } else {
                        CompactNavigationView(
                            state = state,
                            activeItemsCount = activeItems.size,
                            currentIndex = currentIndex
                        )
                    }
                }

                is IslandState.Battery -> {
                    // Charging Connected Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${state.level}% Charging",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                is IslandState.Ringer -> {
                    // Ringer Mode Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val icon = when (state.mode) {
                            "Silent" -> Icons.Default.NotificationsOff
                            "Vibrate" -> Icons.Default.Vibration
                            else -> Icons.Default.Notifications
                        }
                        val tint = when (state.mode) {
                            "Silent" -> Color(0xFFFF453A)
                            "Vibrate" -> Color(0xFFFF9500)
                            else -> Color(0xFF34C759)
                        }
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = state.mode,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                is IslandState.Bluetooth -> {
                    // Bluetooth Connected Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Headphones,
                            contentDescription = null,
                            tint = Color(0xFF0A84FF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = state.deviceName,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                is IslandState.QuickControls -> {
                    // Expanded Quick Controls Dashboard
                    QuickControlsView(context = context)
                }
            }
        }
    }
}

@Composable
private fun MultiAppIndicatorDots(count: Int, currentIndex: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.padding(start = 4.dp)
    ) {
        repeat(count.coerceAtMost(4)) { idx ->
            Box(
                modifier = Modifier
                    .size(if (idx == currentIndex) 5.dp else 4.dp)
                    .clip(CircleShape)
                    .background(if (idx == currentIndex) Color.White else Color(0x66FFFFFF))
            )
        }
    }
}

@Composable
private fun ExpandedMediaView(
    state: IslandState.Media,
    context: Context
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Album art vinyl disc
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x662C2C2E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFFFF2D55),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.artist,
                    color = Color(0xFFC7C7CC),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            EqualizerBars(isPlaying = state.isPlaying)
        }

        // Media Playback Controls (Directly accessible on Lock Screen without unlocking)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { IslandNotificationListener.instance?.sendMediaPrevious() }
            )

            // Play / Pause
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { IslandNotificationListener.instance?.sendMediaPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Next
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { IslandNotificationListener.instance?.sendMediaNext() }
            )
        }
    }
}

@Composable
private fun QuickControlsView(context: Context) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Camera shortcut
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x662C2C2E))
                    .clickable {
                        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try { context.startActivity(intent) } catch (_: Exception) {}
                        IslandController.dismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color(0xFF007AFF), modifier = Modifier.size(19.dp))
            }

            // Settings shortcut
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x662C2C2E))
                    .clickable {
                        val intent = Intent(context, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        IslandController.dismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(19.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Swipe sideways to switch apps",
            color = Color(0xFF8E8E93),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EqualizerBars(isPlaying: Boolean) {
    val bar1 = remember { Animatable(0.3f) }
    val bar2 = remember { Animatable(0.7f) }
    val bar3 = remember { Animatable(0.4f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            bar1.animateTo(
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
            )
        } else {
            bar1.snapTo(0.3f)
        }
    }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            bar2.animateTo(
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
            )
        } else {
            bar2.snapTo(0.3f)
        }
    }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            bar3.animateTo(
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(tween(280, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
            )
        } else {
            bar3.snapTo(0.3f)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(modifier = Modifier.width(3.dp).height((bar1.value * 14).dp).clip(CircleShape).background(Color(0xFF34C759)))
        Box(modifier = Modifier.width(3.dp).height((bar2.value * 14).dp).clip(CircleShape).background(Color(0xFF34C759)))
        Box(modifier = Modifier.width(3.dp).height((bar3.value * 14).dp).clip(CircleShape).background(Color(0xFF34C759)))
    }
}

@Composable
private fun CompactNavigationView(
    state: IslandState.Navigation,
    activeItemsCount: Int,
    currentIndex: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            ManeuverBadge(maneuverType = state.maneuverType, size = 18.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${state.distance} • ${state.instruction}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (activeItemsCount > 1) {
            Spacer(modifier = Modifier.width(4.dp))
            MultiAppIndicatorDots(count = activeItemsCount, currentIndex = currentIndex)
        }
    }
}

@Composable
private fun ExpandedNavigationView(
    state: IslandState.Navigation,
    activeItemsCount: Int,
    currentIndex: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        // Top Row: App Label, Distance badge, Multi-app dots
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ManeuverBadge(maneuverType = state.maneuverType, size = 26.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = state.distance,
                        color = Color(0xFF34C759),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = when (state.maneuverType) {
                            ManeuverType.FLYOVER -> "Flyover Warning"
                            ManeuverType.BIFURCATION -> "Bifurcation / Fork Warning"
                            ManeuverType.U_TURN -> "U-Turn Alert"
                            ManeuverType.ROUNDABOUT -> "Roundabout"
                            ManeuverType.DESTINATION -> "Destination Reached"
                            else -> state.appName
                        },
                        color = Color(0xFFAAAAAA),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (activeItemsCount > 1) {
                MultiAppIndicatorDots(count = activeItemsCount, currentIndex = currentIndex)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Center Instruction
        Text(
            text = state.instruction,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (state.etaInfo.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = state.etaInfo,
                color = Color(0xFF30D158),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ManeuverBadge(
    maneuverType: ManeuverType,
    size: Dp
) {
    val bgColor = when (maneuverType) {
        ManeuverType.FLYOVER -> Color(0xFF007AFF)
        ManeuverType.BIFURCATION -> Color(0xFFFF9500)
        ManeuverType.U_TURN -> Color(0xFFFF3B30)
        ManeuverType.DESTINATION -> Color(0xFFFF2D55)
        else -> Color(0xFF34C759)
    }

    val iconVector = when (maneuverType) {
        ManeuverType.TURN_LEFT -> Icons.Default.TurnLeft
        ManeuverType.TURN_RIGHT -> Icons.Default.TurnRight
        ManeuverType.SLIGHT_LEFT -> Icons.Default.TurnLeft
        ManeuverType.SLIGHT_RIGHT -> Icons.Default.TurnRight
        ManeuverType.SHARP_LEFT -> Icons.Default.TurnLeft
        ManeuverType.SHARP_RIGHT -> Icons.Default.TurnRight
        ManeuverType.U_TURN -> Icons.Default.Undo
        ManeuverType.FLYOVER -> Icons.Default.AltRoute
        ManeuverType.BIFURCATION -> Icons.Default.CallSplit
        ManeuverType.ROUNDABOUT -> Icons.Default.RotateRight
        ManeuverType.DESTINATION -> Icons.Default.Place
        ManeuverType.STRAIGHT -> Icons.Default.Straight
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            iconVector,
            contentDescription = maneuverType.name,
            tint = Color.White,
            modifier = Modifier.size(size * 0.65f)
        )
    }
}

@Suppress("DEPRECATION")
private fun vibrate(context: Context, enabled: Boolean) {
    if (!enabled) return
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(25L)
        }
    } catch (_: Exception) {}
}

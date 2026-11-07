package com.custom.dynamicisland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.custom.dynamicisland.model.IslandSettings
import com.custom.dynamicisland.model.IslandState
import com.custom.dynamicisland.model.ManeuverType
import com.custom.dynamicisland.repository.IslandSettingsRepository
import com.custom.dynamicisland.service.IslandController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepo: IslandSettingsRepository,
    hasOverlayPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by settingsRepo.settings.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F12))
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (settings.isEnabled && hasOverlayPermission) Color(0xFF34C759) else Color(0xFFFF453A))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Dynamic Island PRO",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF16161A))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Master Toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C20)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Dynamic Island", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (settings.isEnabled) "Floating pill active over punch-hole" else "Island overlay disabled",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = settings.isEnabled,
                        onCheckedChange = { settingsRepo.updateSettings(settings.copy(isEnabled = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF34C759))
                    )
                }
            }

            // Section 2: Permissions Status
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C20)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Permissions & Setup", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Overlay Permission
                    PermissionRow(
                        title = "Draw Over Other Apps",
                        subtitle = "Required to display the island over native camera",
                        isGranted = hasOverlayPermission,
                        icon = Icons.Default.Layers,
                        onGrantClick = onRequestOverlayPermission
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Notification Permission
                    PermissionRow(
                        title = "Notification & Media Access",
                        subtitle = "Enables music playback controls and message previews",
                        isGranted = hasNotificationPermission,
                        icon = Icons.Default.Notifications,
                        onGrantClick = onRequestNotificationPermission
                    )
                }
            }

            // Section 3: Camera Cutout Calibration (Galaxy F62 Default)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C20)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF007AFF))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Camera Cutout Alignment", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Align the pill exactly over Galaxy F62's front punch-hole camera lens",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Vertical Offset Y
                    Text("Vertical Position (Y): ${settings.offsetY} dp", color = Color.White, fontSize = 13.sp)
                    Slider(
                        value = settings.offsetY.toFloat(),
                        onValueChange = { settingsRepo.updateSettings(settings.copy(offsetY = it.toInt())) },
                        valueRange = 0f..40f,
                        steps = 40,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF007AFF), activeTrackColor = Color(0xFF007AFF))
                    )

                    // Horizontal Offset X
                    Text("Horizontal Center (X): ${settings.offsetX} dp", color = Color.White, fontSize = 13.sp)
                    Slider(
                        value = settings.offsetX.toFloat(),
                        onValueChange = { settingsRepo.updateSettings(settings.copy(offsetX = it.toInt())) },
                        valueRange = -30f..30f,
                        steps = 60,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.Gray)
                    )

                    // Base Width
                    Text("Resting Pill Width: ${settings.baseWidth} dp", color = Color.White, fontSize = 13.sp)
                    Slider(
                        value = settings.baseWidth.toFloat(),
                        onValueChange = { settingsRepo.updateSettings(settings.copy(baseWidth = it.toInt())) },
                        valueRange = 90f..160f,
                        steps = 70,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF34C759), activeTrackColor = Color(0xFF34C759))
                    )

                    // Base Height
                    Text("Resting Pill Height: ${settings.baseHeight} dp", color = Color.White, fontSize = 13.sp)
                    Slider(
                        value = settings.baseHeight.toFloat(),
                        onValueChange = { settingsRepo.updateSettings(settings.copy(baseHeight = it.toInt())) },
                        valueRange = 26f..44f,
                        steps = 18,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFF9500), activeTrackColor = Color(0xFFFF9500))
                    )

                    // Island Translucency (Frosted Glass Opacity)
                    val opacityPercent = (settings.backgroundOpacity * 100).toInt()
                    Text("Island Background Opacity: $opacityPercent%", color = Color.White, fontSize = 13.sp)
                    Slider(
                        value = settings.backgroundOpacity,
                        onValueChange = { settingsRepo.updateSettings(settings.copy(backgroundOpacity = it)) },
                        valueRange = 0.40f..0.95f,
                        steps = 55,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFAF52DE), activeTrackColor = Color(0xFFAF52DE))
                    )
                }
            }

            // Section 4: PRO Features (All Unlocked)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C20)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Dynamic Island Features", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))

                    FeatureToggleRow("Music & Media Visualizer", "Show track info and equalizer bars", settings.enableMedia, Icons.Default.MusicNote, Color(0xFFFF2D55)) {
                        settingsRepo.updateSettings(settings.copy(enableMedia = it))
                    }
                    FeatureToggleRow("Notification Alerts", "Show popups for WhatsApp, SMS, Mail", settings.enableNotifications, Icons.Default.Notifications, Color(0xFF007AFF)) {
                        settingsRepo.updateSettings(settings.copy(enableNotifications = it))
                    }
                    FeatureToggleRow("Battery & Charging Bolt", "Show animated charging pill when plugged", settings.enableCharging, Icons.Default.BatteryChargingFull, Color(0xFF34C759)) {
                        settingsRepo.updateSettings(settings.copy(enableCharging = it))
                    }
                    FeatureToggleRow("Ringer Mode Status", "Show Silent, Vibrate, Ring pills", settings.enableRinger, Icons.Default.Vibration, Color(0xFFFF9500)) {
                        settingsRepo.updateSettings(settings.copy(enableRinger = it))
                    }
                    FeatureToggleRow("Bluetooth Connections", "Show connected headphones / Buds", settings.enableBluetooth, Icons.Default.Headphones, Color(0xFF0A84FF)) {
                        settingsRepo.updateSettings(settings.copy(enableBluetooth = it))
                    }
                    FeatureToggleRow("Show on Lock Screen", "Display over Keyguard & control music without unlocking", settings.showOnLockScreen, Icons.Default.Lock, Color(0xFF5856D6)) {
                        settingsRepo.updateSettings(settings.copy(showOnLockScreen = it))
                    }
                    FeatureToggleRow("Turn-by-Turn Navigation", "Google Maps, Mapple, Waze (enlarges at turns, auto-shrinks 4s)", settings.enableNavigation, Icons.Default.Navigation, Color(0xFF30D158)) {
                        settingsRepo.updateSettings(settings.copy(enableNavigation = it))
                    }
                    FeatureToggleRow("Hide Island When Idle", "Disappear when apps close, reappear when new eligible app opens", settings.hideWhenIdle, Icons.Default.Layers, Color(0xFF64D2FF)) {
                        settingsRepo.updateSettings(settings.copy(hideWhenIdle = it))
                    }
                }
            }

            // Section 5: Test Triggers
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C20)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Test Island Triggers", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                IslandController.showState(
                                    IslandState.Battery(level = 88, isCharging = true, isPlugged = true),
                                    autoDismissMs = 3800L
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Charging", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                IslandController.showState(
                                    IslandState.Notification(
                                        appName = "WhatsApp",
                                        packageName = "com.whatsapp",
                                        title = "Sarah",
                                        message = "Are you joining the meeting?"
                                    ),
                                    autoDismissMs = 4500L
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Message", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                IslandController.updateMedia(
                                    IslandState.Media(
                                        title = "Starboy",
                                        artist = "The Weeknd",
                                        isPlaying = true,
                                        packageName = "com.spotify.music",
                                        isExpanded = false
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Music", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                IslandController.showState(
                                    IslandState.Bluetooth(deviceName = "Galaxy Buds2 Pro", isConnected = true),
                                    autoDismissMs = 3500L
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Bluetooth", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                IslandController.showState(
                                    IslandState.Call(callerName = "Dr. Brejesh (Incoming)", isIncoming = true),
                                    autoDismissMs = 6000L
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Incoming Call", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                IslandController.updateMedia(
                                    IslandState.Media(
                                        title = "Blinding Lights",
                                        artist = "The Weeknd",
                                        isPlaying = true,
                                        packageName = "com.spotify.music",
                                        isExpanded = false
                                    )
                                )
                                IslandController.showState(
                                    IslandState.Notification(
                                        appName = "WhatsApp",
                                        packageName = "com.whatsapp",
                                        title = "Alex",
                                        message = "Swipe left/right to switch apps!"
                                    ),
                                    autoDismissMs = 12000L
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Multi-App Test", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                IslandController.updateNavigation(
                                    IslandState.Navigation(
                                        appName = "Google Maps",
                                        packageName = "com.google.android.apps.maps",
                                        instruction = "Take flyover towards Airport",
                                        distance = "500 m",
                                        etaInfo = "22 min • 14 km",
                                        maneuverType = ManeuverType.FLYOVER,
                                        isEnlarged = true
                                    ),
                                    autoShrinkMs = 4000L
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Flyover (4s)", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                IslandController.updateNavigation(
                                    IslandState.Navigation(
                                        appName = "Mapple",
                                        packageName = "com.mmi.maps",
                                        instruction = "Keep right at bifurcation towards Expressway",
                                        distance = "150 m",
                                        etaInfo = "18 min • 11 km",
                                        maneuverType = ManeuverType.BIFURCATION,
                                        isEnlarged = true
                                    ),
                                    autoShrinkMs = 4000L
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Bifurcation (4s)", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                IslandController.updateNavigation(
                                    IslandState.Navigation(
                                        appName = "Google Maps",
                                        packageName = "com.google.android.apps.maps",
                                        instruction = "Turn right onto MG Road",
                                        distance = "200 m",
                                        etaInfo = "12 min • 6.5 km",
                                        maneuverType = ManeuverType.TURN_RIGHT,
                                        isEnlarged = true
                                    ),
                                    autoShrinkMs = 4000L
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Turn Warning", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                IslandController.updateNavigation(
                                    IslandState.Navigation(
                                        appName = "Maps",
                                        packageName = "com.google.android.apps.maps",
                                        instruction = "Take flyover in 300 m",
                                        distance = "300 m",
                                        etaInfo = "15 min • 9 km",
                                        maneuverType = ManeuverType.FLYOVER,
                                        isEnlarged = true
                                    ),
                                    autoShrinkMs = 4000L
                                )
                                LockScreenIslandActivity.show(context)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Lock Screen Map", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2C2C2E))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Navigation updates enlarge at every turn/flyover/bifurcation and auto-shrink in 4s. Visible on lock screen over Keyguard. Swipe sideways to switch apps.",
                                color = Color(0xFFE5E5EA),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Section 6: Zero Spyware / Privacy Verified Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14261B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("100% Offline & Ad-Free", color = Color(0xFF34C759), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "0 Ads • 0 Telemetry • 0 Trackers • Zero Internet Permission",
                            color = Color(0xFFC7E5CD),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    icon: ImageVector,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (isGranted) Color(0xFF34C759) else Color(0xFFFF9500), modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = Color.Gray, fontSize = 11.sp)
            }
        }
        if (isGranted) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Granted", tint = Color(0xFF34C759), modifier = Modifier.size(20.dp))
        } else {
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Grant", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FeatureToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: ImageVector,
    iconTint: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = Color.Gray, fontSize = 11.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF34C759))
        )
    }
}

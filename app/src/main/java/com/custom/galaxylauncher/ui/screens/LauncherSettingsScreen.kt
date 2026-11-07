package com.custom.galaxylauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.custom.galaxylauncher.gesture.MotionGestureService
import com.custom.galaxylauncher.model.GestureSettings
import com.custom.galaxylauncher.repository.GestureSettingsRepository
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherSettingsScreen(
    settingsRepo: GestureSettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by settingsRepo.settings.collectAsState()
    val liveSensorData by MotionGestureService.liveSensorData.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F12))
    ) {
        TopAppBar(
            title = {
                Text(
                    "Launcher Settings",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF18181C))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Gestures Activation
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF007AFF))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Motion Gestures",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Chop-Chop toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FlashlightOn, contentDescription = null, tint = Color(0xFFFFD60A))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chop-Chop for Flashlight", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                            Text(
                                "Rapid double downward chop toggles the rear torch",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = settings.chopEnabled,
                            onCheckedChange = { settingsRepo.updateSettings(settings.copy(chopEnabled = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF34C759))
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Wrist twist toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF007AFF))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Double Wrist Twist for Camera", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                            Text(
                                "Quick double twist of wrist immediately launches camera",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = settings.twistEnabled,
                            onCheckedChange = { settingsRepo.updateSettings(settings.copy(twistEnabled = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF34C759))
                        )
                    }
                }
            }

            // Section 2: Motion Arc Sensitivity & Pocket Protection
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFFFF9500))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Arc Distance & Pocket Protection",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        "Enforces minimum angular rotation distance to eliminate false pocket triggers",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Chop-Chop Arc Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Chop-Chop Min Arc: ${settings.chopMinArcDegrees.toInt()}°",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            if (settings.chopMinArcDegrees >= 45f) "Pocket Safe" else "Sensitive",
                            color = if (settings.chopMinArcDegrees >= 45f) Color(0xFF34C759) else Color(0xFFFFD60A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Must swing through at least ${settings.chopMinArcDegrees.toInt()}° twice in quick succession. Minor pocket shifts are discarded.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = settings.chopMinArcDegrees,
                        onValueChange = { settingsRepo.updateSettings(settings.copy(chopMinArcDegrees = it)) },
                        valueRange = 30f..80f,
                        steps = 50,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD60A), activeTrackColor = Color(0xFFFFD60A))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Wrist Twist Arc Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Wrist Twist Min Arc: ${settings.twistMinArcDegrees.toInt()}°",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            if (settings.twistMinArcDegrees >= 60f) "Pocket Safe" else "Sensitive",
                            color = if (settings.twistMinArcDegrees >= 60f) Color(0xFF34C759) else Color(0xFFFFD60A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Must rotate wrist through at least ${settings.twistMinArcDegrees.toInt()}° arc flick and reversal to open camera.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = settings.twistMinArcDegrees,
                        onValueChange = { settingsRepo.updateSettings(settings.copy(twistMinArcDegrees = it)) },
                        valueRange = 40f..100f,
                        steps = 60,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF007AFF), activeTrackColor = Color(0xFF007AFF))
                    )
                }
            }

            // Section 2: 3-Axis Gyroscope Sensitivity Tuning
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sensors, contentDescription = null, tint = Color(0xFF34C759))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Gyroscope 3-Axis Sensitivity",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        "Fine-tune gyroscope plane response to fit your natural hand motions",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // X-Axis (Pitch)
                    Text(
                        "X-Axis (Pitch / Forward-Back): ${String.format("%.1fx", settings.gyroSensitivityX)}",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Slider(
                        value = settings.gyroSensitivityX,
                        onValueChange = { settingsRepo.updateSettings(settings.copy(gyroSensitivityX = it)) },
                        valueRange = 0.2f..3.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF007AFF), activeTrackColor = Color(0xFF007AFF))
                    )

                    // Y-Axis (Roll)
                    Text(
                        "Y-Axis (Roll / Wrist Twist): ${String.format("%.1fx", settings.gyroSensitivityY)}",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Slider(
                        value = settings.gyroSensitivityY,
                        onValueChange = { settingsRepo.updateSettings(settings.copy(gyroSensitivityY = it)) },
                        valueRange = 0.2f..3.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF34C759), activeTrackColor = Color(0xFF34C759))
                    )

                    // Z-Axis (Yaw)
                    Text(
                        "Z-Axis (Yaw / Flat Rotation): ${String.format("%.1fx", settings.gyroSensitivityZ)}",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Slider(
                        value = settings.gyroSensitivityZ,
                        onValueChange = { settingsRepo.updateSettings(settings.copy(gyroSensitivityZ = it)) },
                        valueRange = 0.2f..3.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFF9500), activeTrackColor = Color(0xFFFF9500))
                    )
                }
            }

            // Section 3: Live Sensor Preview & Calibration Meter
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Live Sensor Calibration Meter",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Chop or twist phone now to verify sensor response in real time",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SensorMeterRow(
                        label = "Gyro X",
                        value = liveSensorData.gyroX * settings.gyroSensitivityX,
                        maxVal = 10f,
                        color = Color(0xFF007AFF)
                    )
                    SensorMeterRow(
                        label = "Gyro Y",
                        value = liveSensorData.gyroY * settings.gyroSensitivityY,
                        maxVal = 10f,
                        color = Color(0xFF34C759)
                    )
                    SensorMeterRow(
                        label = "Gyro Z",
                        value = liveSensorData.gyroZ * settings.gyroSensitivityZ,
                        maxVal = 10f,
                        color = Color(0xFFFF9500)
                    )
                }
            }

            // Section 4: Dynamic Island Alignment
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Dynamic Island Camera Alignment",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Align the pill exactly over Galaxy F62's front punch-hole lens",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Vertical offset
                    Text("Vertical Offset (Y): ${settings.islandOffsetY} dp", color = Color.White, fontSize = 13.sp)
                    Slider(
                        value = settings.islandOffsetY.toFloat(),
                        onValueChange = { settingsRepo.updateSettings(settings.copy(islandOffsetY = it.toInt())) },
                        valueRange = -20f..30f,
                        steps = 50,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.Gray)
                    )

                    // Horizontal offset
                    Text("Horizontal Offset (X): ${settings.islandOffsetX} dp", color = Color.White, fontSize = 13.sp)
                    Slider(
                        value = settings.islandOffsetX.toFloat(),
                        onValueChange = { settingsRepo.updateSettings(settings.copy(islandOffsetX = it.toInt())) },
                        valueRange = -30f..30f,
                        steps = 60,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.Gray)
                    )

                    // Pill Base Width
                    Text("Pill Width: ${settings.islandBaseWidth} dp", color = Color.White, fontSize = 13.sp)
                    Slider(
                        value = settings.islandBaseWidth.toFloat(),
                        onValueChange = { settingsRepo.updateSettings(settings.copy(islandBaseWidth = it.toInt())) },
                        valueRange = 80f..160f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.Gray)
                    )
                }
            }

            // Section 5: Privacy & Zero Spyware Audit Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF152A1C)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Safe",
                        tint = Color(0xFF34C759),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            "Clean Architecture Verified",
                            color = Color(0xFF34C759),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "0 Trackers • 0 Ad Networks • 100% On-Device Sensor Computing • Minimal Clean Permissions",
                            color = Color(0xFFC7E5CD),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SensorMeterRow(label: String, value: Float, maxVal: Float, color: Color) {
    val progress = (abs(value) / maxVal).coerceIn(0f, 1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 12.sp, modifier = Modifier.width(55.dp))
        LinearProgressIndicator(
            progress = { progress },
            color = color,
            trackColor = Color(0xFF2C2C2E),
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = String.format("%.1f", value),
            color = Color.Gray,
            fontSize = 11.sp,
            modifier = Modifier.width(35.dp)
        )
    }
}

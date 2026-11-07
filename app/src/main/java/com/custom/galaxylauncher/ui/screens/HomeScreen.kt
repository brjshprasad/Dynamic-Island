package com.custom.galaxylauncher.ui.screens

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.custom.galaxylauncher.dynamicisland.CutoutGeometryHelper
import com.custom.galaxylauncher.dynamicisland.DynamicIslandView
import com.custom.galaxylauncher.model.AppInfo
import com.custom.galaxylauncher.repository.GestureSettingsRepository
import com.custom.galaxylauncher.ui.components.AppIconView
import com.custom.galaxylauncher.ui.components.DockRibbon
import kotlin.math.max

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    apps: List<AppInfo>,
    settingsRepo: GestureSettingsRepository,
    onAppClick: (AppInfo) -> Unit,
    onOpenAllApps: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleTorch: () -> Unit,
    onLaunchCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val settings by settingsRepo.settings.collectAsState()

    // Galaxy F62 front camera position calculated with user offset
    val cutoutPosition = remember(activity, settings.islandOffsetX, settings.islandOffsetY) {
        if (activity != null) {
            CutoutGeometryHelper.getCutoutPosition(activity, settings.islandOffsetX, settings.islandOffsetY)
        } else {
            CutoutGeometryHelper.getCutoutPosition(Activity(), settings.islandOffsetX, settings.islandOffsetY)
        }
    }

    // Split apps: 4 apps pinned to bottom dock, remaining apps distributed across pages (16 per page)
    val dockApps = remember(apps) { apps.take(4) }
    val remainingApps = remember(apps) { apps.drop(4) }

    val appsPerPage = 16
    val pageCount = remember(remainingApps) {
        max(2, (remainingApps.size + appsPerPage - 1) / appsPerPage)
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Deep Slate AMOLED gradient
                        Color(0xFF020617)
                    )
                )
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -30f) {
                        onOpenAllApps() // Swipe up opens App Drawer
                    } else if (dragAmount > 45f) {
                        onOpenSettings() // Swipe down opens Settings
                    }
                }
            }
    ) {
        // Main Home Screen Layout (Pager, Page Indicator, Search Pill, Dock)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top spacing for status bar / Dynamic Island clearance
            Spacer(modifier = Modifier.height(72.dp))

            // Multi-page Desktop HorizontalPager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val pageApps = remember(remainingApps, page) {
                    val startIndex = page * appsPerPage
                    remainingApps.drop(startIndex).take(appsPerPage)
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(pageApps, key = { "${it.packageName}/${it.activityName}" }) { app ->
                        AppIconView(
                            app = app,
                            onClick = { onAppClick(app) },
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            // Bottom Section: Page Indicator + Small Search Box + Dock
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Multi-page indicator dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    repeat(pageCount) { iteration ->
                        val isSelected = pagerState.currentPage == iteration
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 7.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else Color(0x66FFFFFF))
                        )
                    }
                }

                // Small Search Box (Click opens App Drawer with Keyboard focused)
                Box(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x33FFFFFF)) // Translucent iOS search capsule
                        .border(0.8.dp, Color(0x22FFFFFF), RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onOpenSearch
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Search",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Fixed Bottom Dock Ribbon (4 priority apps)
                DockRibbon(
                    dockApps = dockApps,
                    onAppClick = onAppClick,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        // DYNAMIC ISLAND: Top Layer, persistent across all desktop pages
        DynamicIslandView(
            topMargin = cutoutPosition.topMarginDp,
            horizontalOffset = cutoutPosition.centerOffsetXDp,
            baseWidth = settings.islandBaseWidth.dp,
            baseHeight = settings.islandBaseHeight.dp,
            onToggleTorch = onToggleTorch,
            onLaunchCamera = onLaunchCamera,
            onOpenSettings = onOpenSettings,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

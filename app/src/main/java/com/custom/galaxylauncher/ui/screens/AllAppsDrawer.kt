package com.custom.galaxylauncher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.custom.galaxylauncher.model.AppInfo
import com.custom.galaxylauncher.repository.UsageTracker
import com.custom.galaxylauncher.ui.components.AppIconView

@Composable
fun AllAppsDrawer(
    apps: List<AppInfo>,
    usageTracker: UsageTracker,
    autoFocusKeyboard: Boolean = false,
    onAppClick: (AppInfo) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Frequently used apps for the top 2 rows (8 apps total: 4 columns x 2 rows)
    val frequentlyUsedApps = remember(apps) {
        usageTracker.getFrequentlyUsedApps(apps, count = 8)
    }

    // Alphabetically sorted remaining/all apps
    val alphabeticalApps = remember(apps) {
        apps.sortedBy { it.label.lowercase() }
    }

    val filteredApps = remember(searchQuery, alphabeticalApps) {
        if (searchQuery.isBlank()) {
            alphabeticalApps
        } else {
            alphabeticalApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Auto-focus keyboard if launched from Home screen search pill
    LaunchedEffect(autoFocusKeyboard) {
        if (autoFocusKeyboard) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // BACK GESTURE HANDLING:
    // 1st back press: If search query active or keyboard open, clear search & dismiss keyboard
    // 2nd back press: Return to Home Screen
    BackHandler(enabled = true) {
        if (searchQuery.isNotEmpty()) {
            searchQuery = ""
            focusManager.clearFocus()
            keyboardController?.hide()
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
            onCloseDrawer()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xF010141E)) // Deep dark frosted backdrop
            .padding(top = 46.dp, start = 16.dp, end = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search desired app...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.LightGray)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.LightGray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0x33FFFFFF),
                    unfocusedContainerColor = Color(0x22FFFFFF),
                    focusedBorderColor = Color(0x44FFFFFF),
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // If user is searching, show direct search results
                if (searchQuery.isNotBlank()) {
                    item(span = { GridItemSpan(4) }) {
                        Text(
                            text = "Search Results (${filteredApps.size})",
                            color = Color(0xFF8E8E93),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                        )
                    }

                    items(filteredApps, key = { "search_${it.packageName}/${it.activityName}" }) { app ->
                        AppIconView(
                            app = app,
                            onClick = {
                                usageTracker.recordAppLaunch(app.packageName)
                                onAppClick(app)
                            },
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                } else {
                    // SECTION 1: FREQUENTLY USED APPS (TOP 2 ROWS = 8 APPS)
                    if (frequentlyUsedApps.isNotEmpty()) {
                        item(span = { GridItemSpan(4) }) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD60A),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "FREQUENTLY USED",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }

                        items(frequentlyUsedApps, key = { "freq_${it.packageName}/${it.activityName}" }) { app ->
                            AppIconView(
                                app = app,
                                onClick = {
                                    usageTracker.recordAppLaunch(app.packageName)
                                    onAppClick(app)
                                },
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        item(span = { GridItemSpan(4) }) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // SECTION 2: ALL INSTALLED APPS (ALPHABETICAL ORDER)
                    item(span = { GridItemSpan(4) }) {
                        Text(
                            text = "ALL APPS",
                            color = Color(0xFF8E8E93),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                        )
                    }

                    items(alphabeticalApps, key = { "all_${it.packageName}/${it.activityName}" }) { app ->
                        AppIconView(
                            app = app,
                            onClick = {
                                usageTracker.recordAppLaunch(app.packageName)
                                onAppClick(app)
                            },
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

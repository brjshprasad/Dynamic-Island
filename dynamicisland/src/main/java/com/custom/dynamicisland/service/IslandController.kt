package com.custom.dynamicisland.service

import com.custom.dynamicisland.model.IslandState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Central State Manager for Dynamic Island.
 *
 * Supports:
 * - Multiple simultaneous active apps (Media, Call, Notification, Quick Controls).
 * - Side-swipe navigation to switch focus between running apps.
 * - Non-intrusive compact notification & call alerts (no auto-enlargement).
 * - Translucent background layout management.
 */
object IslandController {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var autoDismissJob: Job? = null

    // Multi-activity stack: tracks all running activities associated with Dynamic Island
    private val _activeItems = MutableStateFlow<List<IslandState>>(listOf(IslandState.Idle))
    val activeItems: StateFlow<List<IslandState>> = _activeItems.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _currentState = MutableStateFlow<IslandState>(IslandState.Idle)
    val currentState: StateFlow<IslandState> = _currentState.asStateFlow()

    // Persistent media state when music is active in background
    private var persistentMediaState: IslandState.Media? = null

    // Persistent navigation state when turn-by-turn navigation is active
    private var persistentNavState: IslandState.Navigation? = null
    private var navAutoShrinkJob: Job? = null

    // Callback to tell WindowManager to resize overlay between resting pill and expanded card
    var onLayoutExpansionChanged: ((isExpanded: Boolean) -> Unit)? = null

    // Callback when navigation maneuver updates (e.g. for waking lock screen)
    var onNavigationManeuverWarning: ((nav: IslandState.Navigation) -> Unit)? = null

    /**
     * Show a new state (e.g. notification, call, battery alert).
     * Added to active stack without auto-enlarging into full card.
     */
    fun showState(state: IslandState, autoDismissMs: Long = 4500L) {
        autoDismissJob?.cancel()

        val currentList = _activeItems.value.toMutableList()
        // Remove Idle if real activities exist
        currentList.removeAll { it is IslandState.Idle }

        // Remove any old item of the same type
        when (state) {
            is IslandState.Notification -> currentList.removeAll { it is IslandState.Notification }
            is IslandState.Call -> currentList.removeAll { it is IslandState.Call }
            is IslandState.Navigation -> currentList.removeAll { it is IslandState.Navigation }
            is IslandState.Battery -> currentList.removeAll { it is IslandState.Battery }
            is IslandState.Ringer -> currentList.removeAll { it is IslandState.Ringer }
            is IslandState.Bluetooth -> currentList.removeAll { it is IslandState.Bluetooth }
            is IslandState.QuickControls -> currentList.removeAll { it is IslandState.QuickControls }
            else -> {}
        }

        // Insert new item at head so it's focused
        currentList.add(0, state)
        _activeItems.value = currentList
        _currentIndex.value = 0
        updateActiveSelection()

        if (autoDismissMs > 0L) {
            autoDismissJob = scope.launch {
                delay(autoDismissMs)
                dismissItem(state)
            }
        }
    }

    /**
     * Update active media player state.
     * Stored in active items stack so user can side-swipe between Music and other apps.
     */
    fun updateMedia(media: IslandState.Media) {
        persistentMediaState = media

        val currentList = _activeItems.value.toMutableList()
        currentList.removeAll { it is IslandState.Idle }

        val existingIndex = currentList.indexOfFirst { it is IslandState.Media }
        if (existingIndex >= 0) {
            currentList[existingIndex] = media
        } else {
            // Add media to active apps stack
            currentList.add(media)
        }

        _activeItems.value = currentList
        if (_currentState.value is IslandState.Idle || _currentState.value is IslandState.Media) {
            val mediaIndex = currentList.indexOfFirst { it is IslandState.Media }
            if (mediaIndex >= 0) {
                _currentIndex.value = mediaIndex
            }
        }
        updateActiveSelection()
    }

    fun clearMedia() {
        persistentMediaState = null
        val currentList = _activeItems.value.toMutableList()
        currentList.removeAll { it is IslandState.Media }
        if (currentList.isEmpty()) {
            if (persistentNavState != null) {
                currentList.add(persistentNavState!!)
            } else {
                currentList.add(IslandState.Idle)
            }
        }
        _activeItems.value = currentList
        _currentIndex.value = 0
        updateActiveSelection()
    }

    /**
     * Update active turn-by-turn navigation state (Google Maps, Mapple, OsmAnd, Waze).
     * Automatically enlarges at turns, flyovers, and bifurcations.
     * Auto-shrinks back to compact pill after autoShrinkMs (default 4000ms = 4s) if no touch input.
     */
    fun updateNavigation(nav: IslandState.Navigation, autoShrinkMs: Long = 4000L) {
        persistentNavState = nav

        val currentList = _activeItems.value.toMutableList()
        currentList.removeAll { it is IslandState.Idle }

        val existingIndex = currentList.indexOfFirst { it is IslandState.Navigation }
        if (existingIndex >= 0) {
            currentList[existingIndex] = nav
        } else {
            // Place navigation at head of active items
            currentList.add(0, nav)
        }

        _activeItems.value = currentList
        val navIndex = currentList.indexOfFirst { it is IslandState.Navigation }
        if (nav.isEnlarged && navIndex >= 0) {
            _currentIndex.value = navIndex
        }

        updateActiveSelection()

        // Handle auto-shrink timer: auto-shrinks to compact pill after 4 seconds
        navAutoShrinkJob?.cancel()
        if (nav.isEnlarged && autoShrinkMs > 0L) {
            navAutoShrinkJob = scope.launch {
                delay(autoShrinkMs)
                shrinkNavigation()
            }
        }

        if (nav.isEnlarged) {
            onNavigationManeuverWarning?.invoke(nav)
        }
    }

    /**
     * Smoothly collapses navigation card back to compact resting pill.
     */
    fun shrinkNavigation() {
        navAutoShrinkJob?.cancel()
        val nav = persistentNavState ?: (_currentState.value as? IslandState.Navigation) ?: return
        if (!nav.isEnlarged) return

        val compactNav = nav.copy(isEnlarged = false)
        persistentNavState = compactNav

        val currentList = _activeItems.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it is IslandState.Navigation }
        if (existingIndex >= 0) {
            currentList[existingIndex] = compactNav
            _activeItems.value = currentList
        }
        updateActiveSelection()
    }

    /**
     * Clear navigation when navigation ends or notification is removed.
     */
    fun clearNavigation() {
        navAutoShrinkJob?.cancel()
        persistentNavState = null
        val currentList = _activeItems.value.toMutableList()
        currentList.removeAll { it is IslandState.Navigation }
        if (currentList.isEmpty()) {
            if (persistentMediaState != null) {
                currentList.add(persistentMediaState!!.copy(isExpanded = false))
            } else {
                currentList.add(IslandState.Idle)
            }
        }
        _activeItems.value = currentList
        _currentIndex.value = 0
        updateActiveSelection()
    }

    /**
     * Expand media on touch into full translucent card.
     */
    fun expandMedia() {
        val current = persistentMediaState ?: (_currentState.value as? IslandState.Media) ?: return
        val expanded = current.copy(isExpanded = true)
        persistentMediaState = expanded
        updateMedia(expanded)
    }

    /**
     * Expand navigation on touch into detailed maneuver card.
     */
    fun expandNavigation() {
        val current = persistentNavState ?: (_currentState.value as? IslandState.Navigation) ?: return
        val expanded = current.copy(isEnlarged = true)
        persistentNavState = expanded
        updateNavigation(expanded, autoShrinkMs = 5000L)
    }

    /**
     * Remove dismissed notification immediately to eliminate ghost notifications.
     */
    fun dismissNotificationByKey(key: String, packageName: String) {
        val currentList = _activeItems.value.toMutableList()
        val removed = currentList.removeAll {
            it is IslandState.Notification && (it.notificationKey == key || it.packageName == packageName)
        }
        if (removed) {
            if (currentList.isEmpty()) {
                if (persistentNavState != null) {
                    currentList.add(persistentNavState!!.copy(isEnlarged = false))
                } else if (persistentMediaState != null) {
                    currentList.add(persistentMediaState!!.copy(isExpanded = false))
                } else {
                    currentList.add(IslandState.Idle)
                }
            }
            _activeItems.value = currentList
            _currentIndex.value = _currentIndex.value.coerceIn(0, currentList.size - 1)
            updateActiveSelection()
        }
    }

    /**
     * Remove call imprint when call ends.
     */
    fun dismissCall() {
        val currentList = _activeItems.value.toMutableList()
        val removed = currentList.removeAll { it is IslandState.Call }
        if (removed) {
            if (currentList.isEmpty()) {
                if (persistentNavState != null) {
                    currentList.add(persistentNavState!!.copy(isEnlarged = false))
                } else if (persistentMediaState != null) {
                    currentList.add(persistentMediaState!!.copy(isExpanded = false))
                } else {
                    currentList.add(IslandState.Idle)
                }
            }
            _activeItems.value = currentList
            _currentIndex.value = _currentIndex.value.coerceIn(0, currentList.size - 1)
            updateActiveSelection()
        }
    }

    /**
     * Side-Swipe Left: switch to next active app/activity
     */
    fun nextItem() {
        val list = _activeItems.value
        if (list.size <= 1) {
            // If only 1 item, side swipe can open quick controls
            if (list.firstOrNull() is IslandState.Media) {
                showState(IslandState.QuickControls(), autoDismissMs = 4500L)
            }
            return
        }
        val nextIdx = (_currentIndex.value + 1) % list.size
        _currentIndex.value = nextIdx
        updateActiveSelection()
    }

    /**
     * Side-Swipe Right: switch to previous active app/activity
     */
    fun previousItem() {
        val list = _activeItems.value
        if (list.size <= 1) {
            if (list.firstOrNull() is IslandState.Media) {
                showState(IslandState.QuickControls(), autoDismissMs = 4500L)
            }
            return
        }
        val prevIdx = if (_currentIndex.value - 1 < 0) list.size - 1 else _currentIndex.value - 1
        _currentIndex.value = prevIdx
        updateActiveSelection()
    }

    /**
     * Swipe up to dismiss / collapse current alert item
     */
    fun dismissCurrentItem() {
        val current = _currentState.value
        if (current is IslandState.Media && current.isExpanded) {
            // Collapse expanded media back to compact pill
            toggleMediaExpansion()
        } else if (current is IslandState.Navigation && current.isEnlarged) {
            // Shrink navigation back to compact resting pill
            shrinkNavigation()
        } else {
            dismissItem(current)
        }
    }

    fun dismissItem(item: IslandState) {
        val currentList = _activeItems.value.toMutableList()
        currentList.remove(item)
        if (item is IslandState.Navigation) {
            navAutoShrinkJob?.cancel()
            persistentNavState = null
        }
        if (currentList.isEmpty()) {
            if (persistentNavState != null) {
                currentList.add(persistentNavState!!.copy(isEnlarged = false))
            } else if (persistentMediaState != null) {
                currentList.add(persistentMediaState!!.copy(isExpanded = false))
            } else {
                currentList.add(IslandState.Idle)
            }
        }
        _activeItems.value = currentList
        _currentIndex.value = _currentIndex.value.coerceIn(0, currentList.size - 1)
        updateActiveSelection()
    }

    fun toggleMediaExpansion() {
        val current = _currentState.value
        if (current is IslandState.Media) {
            val next = current.copy(isExpanded = !current.isExpanded)
            persistentMediaState = next
            updateMedia(next)
        } else if (persistentMediaState != null) {
            val expanded = persistentMediaState!!.copy(isExpanded = true)
            persistentMediaState = expanded
            updateMedia(expanded)
        }
    }

    fun dismiss() {
        autoDismissJob?.cancel()
        if (persistentMediaState != null) {
            val restingMedia = persistentMediaState!!.copy(isExpanded = false)
            _activeItems.value = listOf(restingMedia)
            _currentIndex.value = 0
            _currentState.value = restingMedia
            onLayoutExpansionChanged?.invoke(false)
        } else {
            _activeItems.value = listOf(IslandState.Idle)
            _currentIndex.value = 0
            _currentState.value = IslandState.Idle
            onLayoutExpansionChanged?.invoke(false)
        }
    }

    fun reset() {
        autoDismissJob?.cancel()
        navAutoShrinkJob?.cancel()
        persistentMediaState = null
        persistentNavState = null
        _activeItems.value = listOf(IslandState.Idle)
        _currentIndex.value = 0
        _currentState.value = IslandState.Idle
        onLayoutExpansionChanged = null
        onNavigationManeuverWarning = null
    }

    private fun updateActiveSelection() {
        val list = _activeItems.value
        val idx = _currentIndex.value.coerceIn(0, list.size - 1)
        val selected = list.getOrElse(idx) { IslandState.Idle }
        _currentState.value = selected

        // Layout expansion is enabled for QuickControls, user-expanded Media, or enlarged Navigation warnings
        val isExpanded = (selected is IslandState.QuickControls) ||
                (selected is IslandState.Media && selected.isExpanded) ||
                (selected is IslandState.Navigation && selected.isEnlarged)

        onLayoutExpansionChanged?.invoke(isExpanded)
    }
}

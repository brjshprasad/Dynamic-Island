package com.custom.dynamicisland

import com.custom.dynamicisland.model.IslandState
import com.custom.dynamicisland.model.ManeuverType
import com.custom.dynamicisland.service.IslandController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IslandControllerTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        IslandController.reset()
    }

    @After
    fun tearDown() {
        IslandController.reset()
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isIdle() {
        assertEquals(IslandState.Idle, IslandController.currentState.value)
    }

    @Test
    fun showState_updatesCurrentState() {
        val batteryState = IslandState.Battery(level = 85, isCharging = true, isPlugged = true)
        IslandController.showState(batteryState, autoDismissMs = 0L)

        assertEquals(batteryState, IslandController.currentState.value)
    }

    @Test
    fun updateMedia_setsMediaState() {
        val mediaState = IslandState.Media(
            title = "Test Song",
            artist = "Test Artist",
            isPlaying = true,
            packageName = "com.spotify.music"
        )
        IslandController.updateMedia(mediaState)

        assertEquals(mediaState, IslandController.currentState.value)
    }

    @Test
    fun toggleMediaExpansion_togglesState() {
        val mediaState = IslandState.Media(
            title = "Test Song",
            artist = "Test Artist",
            isPlaying = true,
            packageName = "com.spotify.music",
            isExpanded = false
        )
        IslandController.updateMedia(mediaState)

        IslandController.toggleMediaExpansion()
        val expanded = IslandController.currentState.value as IslandState.Media
        assertTrue(expanded.isExpanded)

        IslandController.toggleMediaExpansion()
        val collapsed = IslandController.currentState.value as IslandState.Media
        assertTrue(!collapsed.isExpanded)
    }

    @Test
    fun clearMedia_resetsToIdle() {
        val mediaState = IslandState.Media(
            title = "Test Song",
            artist = "Test Artist",
            isPlaying = true,
            packageName = "com.spotify.music"
        )
        IslandController.updateMedia(mediaState)
        IslandController.clearMedia()

        assertEquals(IslandState.Idle, IslandController.currentState.value)
    }

    @Test
    fun incomingCall_keepsCompactLayoutWithoutExpanding() {
        var expansionReported: Boolean? = null
        IslandController.onLayoutExpansionChanged = { isExpanded ->
            expansionReported = isExpanded
        }

        val mediaState = IslandState.Media(
            title = "Starboy",
            artist = "The Weeknd",
            isPlaying = true,
            packageName = "com.spotify.music"
        )
        IslandController.updateMedia(mediaState)

        val callState = IslandState.Call(callerName = "John Doe", isIncoming = true)
        IslandController.showState(callState, autoDismissMs = 0L)

        // Must display Call pill
        assertEquals(callState, IslandController.currentState.value)
        // Must NOT force full enlargement, preventing obscuring incoming caller UI
        assertEquals(false, expansionReported)
        // Both Call and Media must be in activeItems stack
        assertEquals(2, IslandController.activeItems.value.size)
        assertEquals(callState, IslandController.activeItems.value[0])
        assertEquals(mediaState, IslandController.activeItems.value[1])
    }

    @Test
    fun sideSwipe_navigatesBetweenMultipleActiveApps() {
        val mediaState = IslandState.Media(
            title = "Starboy",
            artist = "The Weeknd",
            isPlaying = true,
            packageName = "com.spotify.music"
        )
        IslandController.updateMedia(mediaState)

        val notifState = IslandState.Notification(
            appName = "WhatsApp",
            packageName = "com.whatsapp",
            title = "Sarah",
            message = "Hello!"
        )
        IslandController.showState(notifState, autoDismissMs = 0L)

        // Initially focused on newly received notification (index 0)
        assertEquals(notifState, IslandController.currentState.value)
        assertEquals(0, IslandController.currentIndex.value)

        // Side swipe left/next -> switches to Media (index 1)
        IslandController.nextItem()
        assertEquals(1, IslandController.currentIndex.value)
        assertEquals(mediaState, IslandController.currentState.value)

        // Side swipe right/previous -> switches back to Notification (index 0)
        IslandController.previousItem()
        assertEquals(0, IslandController.currentIndex.value)
        assertEquals(notifState, IslandController.currentState.value)
    }

    @Test
    fun dismissCurrentItem_revertsToOtherActiveItem() {
        val mediaState = IslandState.Media(
            title = "Starboy",
            artist = "The Weeknd",
            isPlaying = true,
            packageName = "com.spotify.music"
        )
        IslandController.updateMedia(mediaState)

        val callState = IslandState.Call(callerName = "Alice", isIncoming = true)
        IslandController.showState(callState, autoDismissMs = 0L)

        assertEquals(callState, IslandController.currentState.value)

        // Dismiss the call -> should seamlessly fall back to the playing media
        IslandController.dismissCurrentItem()
        assertEquals(mediaState, IslandController.currentState.value)
        assertEquals(1, IslandController.activeItems.value.size)
    }

    @Test
    fun notification_keepsCompactLayoutWithoutAutoEnlarging() {
        var expansionReported: Boolean? = null
        IslandController.onLayoutExpansionChanged = { isExpanded ->
            expansionReported = isExpanded
        }

        val notif = IslandState.Notification(
            appName = "Gmail",
            packageName = "com.google.android.gm",
            title = "Work",
            message = "New mail"
        )
        IslandController.showState(notif, autoDismissMs = 0L)

        assertEquals(notif, IslandController.currentState.value)
        // Must stay compact
        assertEquals(false, expansionReported)
    }

    @Test
    fun updateNavigation_enlargesLayout_andAutoShrinksAfter4Seconds() = runTest {
        var expansionReported: Boolean? = null
        IslandController.onLayoutExpansionChanged = { isExpanded ->
            expansionReported = isExpanded
        }

        val navState = IslandState.Navigation(
            appName = "Google Maps",
            packageName = "com.google.android.apps.maps",
            instruction = "Take flyover towards Airport",
            distance = "500 m",
            etaInfo = "22 min • 14 km",
            maneuverType = ManeuverType.FLYOVER,
            isEnlarged = true
        )
        IslandController.updateNavigation(navState, autoShrinkMs = 4000L)

        // Must be enlarged immediately upon maneuver warning
        assertEquals(true, expansionReported)
        val currentEnlarged = IslandController.currentState.value as IslandState.Navigation
        assertTrue(currentEnlarged.isEnlarged)
        assertEquals(ManeuverType.FLYOVER, currentEnlarged.maneuverType)

        // Advance virtual time by 4001 ms (exceeding 4 seconds auto-shrink timeout)
        testDispatcher.scheduler.advanceTimeBy(4001L)

        // Should automatically shrink back to compact resting pill
        assertEquals(false, expansionReported)
        val currentShrunk = IslandController.currentState.value as IslandState.Navigation
        assertTrue(!currentShrunk.isEnlarged)
        assertEquals("Take flyover towards Airport", currentShrunk.instruction)
    }

    @Test
    fun navigationManeuverWarning_triggersLockScreenCallback() {
        var warningFired: IslandState.Navigation? = null
        IslandController.onNavigationManeuverWarning = { nav ->
            warningFired = nav
        }

        val bifurcationNav = IslandState.Navigation(
            appName = "Mapple",
            packageName = "com.mmi.maps",
            instruction = "Keep right at bifurcation",
            distance = "150 m",
            maneuverType = ManeuverType.BIFURCATION,
            isEnlarged = true
        )
        IslandController.updateNavigation(bifurcationNav)

        assertEquals(bifurcationNav, warningFired)
    }

    @Test
    fun navigationAndMedia_coexistInActiveStack_andCanBeSwiped() {
        val media = IslandState.Media(
            title = "Starboy",
            artist = "The Weeknd",
            isPlaying = true,
            packageName = "com.spotify.music"
        )
        IslandController.updateMedia(media)

        val nav = IslandState.Navigation(
            appName = "Google Maps",
            packageName = "com.google.android.apps.maps",
            instruction = "Turn right onto MG Road",
            distance = "200 m",
            maneuverType = ManeuverType.TURN_RIGHT,
            isEnlarged = false
        )
        IslandController.updateNavigation(nav)

        // Both Navigation and Media are concurrent in active items stack
        assertEquals(2, IslandController.activeItems.value.size)

        // Navigation is at index 0
        assertEquals(nav, IslandController.currentState.value)

        // Swipe left/next -> switches focus to Media
        IslandController.nextItem()
        assertEquals(media, IslandController.currentState.value)

        // Swipe right/previous -> switches focus back to Navigation
        IslandController.previousItem()
        assertEquals(nav, IslandController.currentState.value)
    }

    @Test
    fun expandMedia_and_expandNavigation_expandsLayoutOnTouch() {
        var expansionReported: Boolean? = null
        IslandController.onLayoutExpansionChanged = { isExpanded ->
            expansionReported = isExpanded
        }

        val media = IslandState.Media(
            title = "Starboy",
            artist = "The Weeknd",
            isPlaying = true,
            packageName = "com.spotify.music",
            isExpanded = false
        )
        IslandController.updateMedia(media)
        assertEquals(false, expansionReported)

        // Expand media on touch
        IslandController.expandMedia()
        assertEquals(true, expansionReported)
        val expandedMedia = IslandController.currentState.value as IslandState.Media
        assertTrue(expandedMedia.isExpanded)

        // Expand navigation on touch
        val nav = IslandState.Navigation(
            appName = "Google Maps",
            packageName = "com.google.android.apps.maps",
            instruction = "Turn right onto MG Road",
            distance = "200 m",
            maneuverType = ManeuverType.TURN_RIGHT,
            isEnlarged = false
        )
        IslandController.updateNavigation(nav)
        assertEquals(false, expansionReported)

        IslandController.expandNavigation()
        assertEquals(true, expansionReported)
        val expandedNav = IslandController.currentState.value as IslandState.Navigation
        assertTrue(expandedNav.isEnlarged)
    }

    @Test
    fun dismissNotificationByKey_eliminatesGhostNotifications() {
        val notif = IslandState.Notification(
            appName = "WhatsApp",
            packageName = "com.whatsapp",
            title = "Sarah",
            message = "Hello",
            notificationKey = "0|com.whatsapp|101|null|1000"
        )
        IslandController.showState(notif, autoDismissMs = 0L)
        assertEquals(notif, IslandController.currentState.value)

        // Notification dismissed in system -> immediately removes from island
        IslandController.dismissNotificationByKey("0|com.whatsapp|101|null|1000", "com.whatsapp")
        assertEquals(IslandState.Idle, IslandController.currentState.value)
        assertEquals(1, IslandController.activeItems.value.size)
        assertEquals(IslandState.Idle, IslandController.activeItems.value[0])
    }

    @Test
    fun closedApp_clearsImprint_andRevertsToIdleOrRemainingApp() {
        val media = IslandState.Media(
            title = "Starboy",
            artist = "The Weeknd",
            isPlaying = true,
            packageName = "com.spotify.music"
        )
        IslandController.updateMedia(media)

        val nav = IslandState.Navigation(
            appName = "Mapple",
            packageName = "com.mmi.maps",
            instruction = "Keep right at fork",
            distance = "300 m",
            maneuverType = ManeuverType.BIFURCATION
        )
        IslandController.updateNavigation(nav)

        // Two apps active
        assertEquals(2, IslandController.activeItems.value.size)

        // User finishes navigation in Maps -> Navigation app closes
        IslandController.clearNavigation()

        // Navigation imprint disappears, Music remains active
        assertEquals(1, IslandController.activeItems.value.size)
        assertEquals(media, IslandController.currentState.value)

        // User closes Spotify -> Music app closes
        IslandController.clearMedia()

        // All imprints disappeared -> reverts cleanly to Idle
        assertEquals(IslandState.Idle, IslandController.currentState.value)
    }
}

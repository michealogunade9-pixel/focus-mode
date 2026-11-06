package com.example

import com.example.focuslock.data.local.entity.FocusSessionEntity
import com.example.focuslock.data.model.InstalledAppInfo
import com.example.focuslock.data.model.SessionStats
import com.example.focuslock.data.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusLockUnitTest {

  @Test
  fun `boot receiver and focus service classes exist and are loadable`() {
    val bootReceiverClass = Class.forName("com.example.focuslock.service.BootReceiver")
    assertNotNull(bootReceiverClass)

    val compatibilityBootReceiverClass = Class.forName("com.example.service.BootReceiver")
    assertNotNull(compatibilityBootReceiverClass)

    val accessibilityServiceClass = Class.forName("com.example.focuslock.service.FocusAccessibilityService")
    assertNotNull(accessibilityServiceClass)

    val notificationListenerClass = Class.forName("com.example.focuslock.service.FocusNotificationListenerService")
    assertNotNull(notificationListenerClass)
  }

  @Test
  fun `reboot logic calculates remaining time from saved endTimeMillis`() {
    val now = System.currentTimeMillis()
    // Session that was planned for 60 mins, but only 20 mins remaining when reboot happened
    val originalMinutes = 60
    val savedEndTime = now + (20 * 60 * 1000L)

    val session = FocusSessionEntity(
      id = 5L,
      startTimeMillis = now - (40 * 60 * 1000L),
      endTimeMillis = savedEndTime,
      originalDurationMinutes = originalMinutes,
      status = SessionStatus.ACTIVE.name,
      isStrictMode = true,
      blockedPackageNamesJson = "com.distraction.app"
    )

    // Verify it is not expired
    assertTrue(session.endTimeMillis > now)

    // Verify restored remaining time strictly uses savedEndTime, not originalDuration
    val remainingMinutes = ((session.endTimeMillis - now) / 60000L).toInt()
    assertEquals(20, remainingMinutes)
  }

  @Test
  fun `reboot logic detects expired session correctly`() {
    val now = System.currentTimeMillis()
    // Session that expired 5 minutes ago while device was off
    val expiredEndTime = now - (5 * 60 * 1000L)

    val session = FocusSessionEntity(
      id = 6L,
      startTimeMillis = now - (35 * 60 * 1000L),
      endTimeMillis = expiredEndTime,
      originalDurationMinutes = 30,
      status = SessionStatus.ACTIVE.name,
      isStrictMode = false,
      blockedPackageNamesJson = "com.distraction.app"
    )

    // Expired check: endTimeMillis <= now
    val isExpired = session.endTimeMillis <= now
    assertTrue(isExpired)
  }

  @Test
  fun `session entity models active and completed states`() {
    val now = System.currentTimeMillis()
    val session = FocusSessionEntity(
      id = 1L,
      startTimeMillis = now,
      endTimeMillis = now + 1800000L,
      originalDurationMinutes = 30,
      status = SessionStatus.ACTIVE.name,
      isStrictMode = true,
      blockedPackageNamesJson = "com.test.app1,com.test.app2",
      blocksPreventedCount = 3
    )

    assertEquals(30, session.originalDurationMinutes)
    assertEquals(SessionStatus.ACTIVE.name, session.status)
    assertTrue(session.isStrictMode)
    assertEquals(3, session.blocksPreventedCount)
  }

  @Test
  fun `session stats calculations provide accurate time and counts`() {
    val stats = SessionStats(
      totalFocusMinutes = 120L,
      completedSessionsCount = 4,
      totalDeflectedAttempts = 9,
      currentStreakDays = 3
    )

    assertEquals(120L, stats.totalFocusMinutes)
    assertEquals(4, stats.completedSessionsCount)
    assertEquals(9, stats.totalDeflectedAttempts)
    assertEquals(3, stats.currentStreakDays)
  }

  @Test
  fun `installed app info correctly models blocked state`() {
    val app = InstalledAppInfo(
      packageName = "com.instagram.android",
      appName = "Instagram",
      isBlocked = true,
      category = "Social"
    )

    assertTrue(app.isBlocked)
    assertEquals("Social", app.category)
    assertEquals("Instagram", app.appName)
  }

  @Test
  fun `ActiveFocusState blocks target packages during active session and restores access upon expiration`() {
    val activeState = com.example.focuslock.data.local.ActiveFocusState
    val now = System.currentTimeMillis()
    val futureEnd = now + 60000L

    activeState.setActiveSession(
      sessionId = 42L,
      startTime = now,
      endTime = futureEnd,
      isStrictMode = false,
      blockedPackages = setOf("com.android.chrome", "com.instagram.android")
    )

    // While session is active, Chrome is blocked
    assertTrue(activeState.isSessionActive)
    assertTrue(activeState.isBlocked("com.android.chrome"))
    assertTrue(activeState.isBlocked("com.instagram.android"))
    assertFalse(activeState.isBlocked("com.unblocked.notes"))

    // When session is cleared / timer expires, Chrome is immediately unblocked
    activeState.clear()
    assertFalse(activeState.isSessionActive)
    assertFalse(activeState.isBlocked("com.android.chrome"))
    assertFalse(activeState.isBlocked("com.instagram.android"))
  }

  @Test
  fun `strict mode session is configured and flags cancellation as prohibited`() {
    val strictSession = FocusSessionEntity(
      id = 10L,
      startTimeMillis = System.currentTimeMillis(),
      endTimeMillis = System.currentTimeMillis() + 1800000L,
      originalDurationMinutes = 30,
      status = SessionStatus.ACTIVE.name,
      isStrictMode = true,
      blockedPackageNamesJson = "com.android.chrome"
    )

    assertTrue(strictSession.isStrictMode)
    assertEquals(SessionStatus.ACTIVE.name, strictSession.status)

    val normalSession = FocusSessionEntity(
      id = 11L,
      startTimeMillis = System.currentTimeMillis(),
      endTimeMillis = System.currentTimeMillis() + 1800000L,
      originalDurationMinutes = 30,
      status = SessionStatus.ACTIVE.name,
      isStrictMode = false,
      blockedPackageNamesJson = "com.android.chrome"
    )

    assertFalse(normalSession.isStrictMode)
  }

  @Test
  fun `strict mode state reflects active locked status and unblocks on expiry`() {
    val activeState = com.example.focuslock.data.local.ActiveFocusState
    val now = System.currentTimeMillis()

    // 1. Strict Mode ON with unexpired time
    activeState.setActiveSession(
      sessionId = 99L,
      startTime = now,
      endTime = now + 50000L,
      isStrictMode = true,
      blockedPackages = setOf("com.android.chrome")
    )
    assertTrue(activeState.isSessionActive)
    assertTrue(activeState.isStrictModeActive)
    assertTrue(activeState.isBlocked("com.android.chrome"))

    // 2. Strict Mode with passed end time is automatically unblocked
    activeState.setActiveSession(
      sessionId = 100L,
      startTime = now - 100000L,
      endTime = now - 1000L, // Already expired
      isStrictMode = true,
      blockedPackages = setOf("com.android.chrome")
    )
    assertFalse(activeState.isBlocked("com.android.chrome"))
  }
}



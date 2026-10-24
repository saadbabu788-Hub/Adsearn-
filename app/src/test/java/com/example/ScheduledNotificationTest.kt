package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.notification.DailyNotificationScheduler
import com.example.notification.ScheduledNotificationPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
class ScheduledNotificationTest {

    @Test
    fun defaultSlots_configuredCorrectly() {
        val slot1 = ScheduledNotificationPrefs.DEFAULT_SLOT_1
        val slot2 = ScheduledNotificationPrefs.DEFAULT_SLOT_2
        val slot3 = ScheduledNotificationPrefs.DEFAULT_SLOT_3

        // Slot 1: 9:00 AM (Money/Reward themed)
        assertEquals(1, slot1.id)
        assertEquals(9, slot1.hour)
        assertEquals(0, slot1.minute)
        assertTrue("Slot 1 title must check rewards safely", slot1.title.contains("Check Your Rewards"))
        assertTrue("Slot 1 message must prompt rewards inspection", slot1.message.contains("Adsearn and check out your rewards"))

        // Slot 2: 12:00 PM (Funny Babu themed)
        assertEquals(2, slot2.id)
        assertEquals(12, slot2.hour)
        assertEquals(0, slot2.minute)
        assertTrue("Slot 2 title must match funny Babu theme", slot2.title.contains("Babu, Mujhe Paise Ki Zarurat Hai!"))
        assertTrue("Slot 2 message must contain Babu body", slot2.message.contains("Babu, mujhe paison ki zarurat hai!"))

        // Slot 3: 5:00 PM (17:00) (Momos themed)
        assertEquals(3, slot3.id)
        assertEquals(17, slot3.hour)
        assertEquals(0, slot3.minute)
        assertTrue("Slot 3 title must match Momos theme", slot3.title.contains("Kya Momos Khaoge?"))
        assertTrue("Slot 3 message must contain momos copy", slot3.message.contains("Kya momos khaoge?"))
    }

    @Test
    fun nextTriggerCalculation_returnsFutureTimeInKolkata() {
        val trigger9am = DailyNotificationScheduler.computeNextTriggerEpochMillis(9, 0)
        val trigger12pm = DailyNotificationScheduler.computeNextTriggerEpochMillis(12, 0)
        val trigger5pm = DailyNotificationScheduler.computeNextTriggerEpochMillis(17, 0)

        val nowMillis = System.currentTimeMillis()
        assertTrue("Trigger for 9 AM must be in future", trigger9am > nowMillis)
        assertTrue("Trigger for 12 PM must be in future", trigger12pm > nowMillis)
        assertTrue("Trigger for 5 PM must be in future", trigger5pm > nowMillis)

        // Verify target time in Asia/Kolkata
        val zone = ZoneId.of(TIMEZONE_ASIA_KOLKATA)
        val dt9am = ZonedDateTime.ofInstant(Instant.ofEpochMilli(trigger9am), zone)
        assertEquals(9, dt9am.hour)
        assertEquals(0, dt9am.minute)

        val dt12pm = ZonedDateTime.ofInstant(Instant.ofEpochMilli(trigger12pm), zone)
        assertEquals(12, dt12pm.hour)
        assertEquals(0, dt12pm.minute)

        val dt5pm = ZonedDateTime.ofInstant(Instant.ofEpochMilli(trigger5pm), zone)
        assertEquals(17, dt5pm.hour)
        assertEquals(0, dt5pm.minute)
    }

    @Test
    fun duplicatePrevention_recordsAndChecksPerDate() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = ScheduledNotificationPrefs(context)

        val testDate = "2026-10-24"
        assertFalse(prefs.hasFiredToday(1, testDate))

        prefs.recordFiredToday(1, testDate)
        assertTrue(prefs.hasFiredToday(1, testDate))
        assertFalse("Different slot must not be marked", prefs.hasFiredToday(2, testDate))
        assertFalse("Different date must not be marked", prefs.hasFiredToday(1, "2026-10-25"))
    }

    @Test
    fun adminPin_defaultsTo7788() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = ScheduledNotificationPrefs(context)
        assertEquals("7788", prefs.adminPin)
    }
}

package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AdStats
import com.example.data.WithdrawalRequest
import com.example.data.WithdrawalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Adsearn", appName)
    }

    @Test
    fun `verify constants values`() {
        assertEquals("Adsearn", APP_NAME)
        assertEquals("com.Adsearn.unityadsdemo", PACKAGE_NAME)
        assertEquals("6209840", UNITY_GAME_ID)
        assertEquals("Rewarded_Android", REWARDED_PLACEMENT_ID)
        assertEquals("Interstitial_Android", INTERSTITIAL_PLACEMENT_ID)
        assertEquals(false, TEST_MODE)
        assertEquals(2.0, ESTIMATED_ECPM_USD, 0.001)
        assertEquals(10, REWARD_COINS_PER_AD)
        assertEquals(3.0, REWARD_INR_PER_AD, 0.001)
        assertEquals(500.0, MIN_WITHDRAWAL_INR, 0.001)
    }

    @Test
    fun `verify ad stats calculation`() {
        val stats = AdStats(
            totalClicks = 5,
            rewardedWatched = 50,
            interstitialsShown = 50,
            adsSkipped = 10,
            totalCoinsEarned = 500,
            loadSuccess = 105,
            loadFailed = 2,
            totalRupeesEarned = 150.0,
            availableRupeesBalance = 150.0
        )
        assertEquals(100, stats.totalImpressions)
        assertEquals(5.0, stats.ctrPercent, 0.001)
        // 100 / 1000 * 2.0 = 0.20 USD
        assertEquals(0.20, stats.estimatedEarningsUsd, 0.0001)
        assertEquals(150.0, stats.availableRupeesBalance, 0.001)
        assertFalse(stats.isEligibleForWithdrawal)
    }

    @Test
    fun `verify withdrawal eligibility and masking`() {
        val eligibleStats = AdStats(availableRupeesBalance = 510.0)
        assertTrue(eligibleStats.isEligibleForWithdrawal)

        val request = WithdrawalRequest(
            amountInr = 500.0,
            upiId = "rajeshkumar@okaxis",
            status = WithdrawalStatus.PENDING
        )
        assertEquals("ra***ar@okaxis", request.maskedUpi)
        assertEquals("Pending", request.status.displayName)
    }
}

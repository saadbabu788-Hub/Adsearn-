package com.example

import com.example.data.AdminSecurity
import com.example.data.AppVersionInfo
import com.example.data.CoinRedemptionTransaction
import com.example.data.ReferralCampaignSettings
import com.example.data.WithdrawalRequest
import com.example.data.WithdrawalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdsearnFeatureTest {

    @Test
    fun adminSecurity_verifiesInitialPasswordSuccessfully() {
        // Must succeed for configured initial password Saad@1234
        assertTrue("Admin password Saad@1234 must verify successfully", AdminSecurity.verifyPassword("Saad@1234"))

        // Must reject wrong passwords
        assertFalse("Wrong password must fail", AdminSecurity.verifyPassword("WrongPassword"))
        assertFalse("Case sensitive password check must fail", AdminSecurity.verifyPassword("saad@1234"))
        assertFalse("Empty password must fail", AdminSecurity.verifyPassword(""))
    }

    @Test
    fun coinRedemption_conversionRulesAccurate() {
        // 20 coins = ₹3
        assertEquals(3.0, CoinRedemptionTransaction.calculateRupees(20), 0.001)
        // 40 coins = ₹6
        assertEquals(6.0, CoinRedemptionTransaction.calculateRupees(40), 0.001)
        // 100 coins = ₹15
        assertEquals(15.0, CoinRedemptionTransaction.calculateRupees(100), 0.001)
        // 200 coins = ₹30
        assertEquals(30.0, CoinRedemptionTransaction.calculateRupees(200), 0.001)

        // Invalid multiples
        assertTrue(CoinRedemptionTransaction.isValidMultiples(20))
        assertTrue(CoinRedemptionTransaction.isValidMultiples(40))
        assertTrue(CoinRedemptionTransaction.isValidMultiples(100))
        assertFalse(CoinRedemptionTransaction.isValidMultiples(15))
        assertFalse(CoinRedemptionTransaction.isValidMultiples(25))
        assertFalse(CoinRedemptionTransaction.isValidMultiples(0))
    }

    @Test
    fun referralSettings_defaultsTo50And5() {
        val settings = ReferralCampaignSettings()
        assertEquals(50.0, settings.firstReferralRewardInr, 0.001)
        assertEquals(5.0, settings.subsequentReferralRewardInr, 0.001)
    }

    @Test
    fun appVersionInfo_detectsOutdatedBuild() {
        val currentBuild = 1
        val upToDateInfo = AppVersionInfo(latestVersionCode = 1, latestVersionName = "1.0")
        assertFalse("Build 1 is not outdated when latest is 1", upToDateInfo.isUpdateAvailable(currentBuild))

        val newVersionInfo = AppVersionInfo(latestVersionCode = 2, latestVersionName = "1.1")
        assertTrue("Build 1 is outdated when latest is 2", newVersionInfo.isUpdateAvailable(currentBuild))
    }

    @Test
    fun withdrawalRequest_maskedUpiAndStatusHandling() {
        val req = WithdrawalRequest(
            amountInr = 500.0,
            upiId = "user123@okhdfcbank",
            status = WithdrawalStatus.PENDING
        )
        assertTrue(req.maskedUpi.contains("@okhdfcbank"))
        assertEquals(500.0, req.amountInr, 0.001)
        assertEquals("Pending", req.status.displayName)
    }

    @Test
    fun fcmPushManager_tokenRegistrationAndDuplicateDetection() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val testToken = "test_fcm_token_12345"

        com.example.notification.FcmPushManager.registerDeviceToken(context, testToken)
        assertEquals(testToken, com.example.notification.FcmPushManager.getFcmToken(context))

        val devices = com.example.notification.FcmPushManager.getRegisteredDevices(context)
        assertTrue("Registered devices must contain newly registered token", devices.any { it.deviceToken == testToken })

        val broadcastId = "broadcast_test_999"
        assertFalse("Message ID should not be duplicate initially", com.example.notification.FcmPushManager.isDuplicateMessage(context, broadcastId))

        com.example.notification.FcmPushManager.markMessageReceived(context, broadcastId)
        assertTrue("Message ID must be flagged as duplicate once marked", com.example.notification.FcmPushManager.isDuplicateMessage(context, broadcastId))
    }
}

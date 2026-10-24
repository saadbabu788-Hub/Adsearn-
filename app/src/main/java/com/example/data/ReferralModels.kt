package com.example.data

import java.util.UUID

data class ReferralRecord(
    val id: String = UUID.randomUUID().toString(),
    val friendCode: String,
    val rewardInr: Double,
    val isFirstReferral: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Credited"
)

data class ReferralCampaignSettings(
    val firstReferralRewardInr: Double = 50.0,
    val subsequentReferralRewardInr: Double = 5.0,
    val minEligibilityRequirement: String = "Friend registers and completes at least 1 ad"
)

data class ReferralState(
    val myReferralCode: String = "",
    val totalReferralsCount: Int = 0,
    val firstReferralClaimed: Boolean = false,
    val totalReferralRupeesCredited: Double = 0.0,
    val referralHistory: List<ReferralRecord> = emptyList(),
    val appliedReferrerCode: String? = null
) {
    val shareableLink: String
        get() = "https://adsearn.app/invite?ref=$myReferralCode"
}

package com.example.data

data class AppVersionInfo(
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0",
    val releaseNotes: String = "Initial stable release: 3x daily scheduled notifications (9 AM, 12 PM, 5 PM), coin redemption system, and UPI payout tracking.",
    val downloadUrl: String = "https://github.com/adsearn/releases/latest",
    val isMandatory: Boolean = false
) {
    fun isUpdateAvailable(currentVersionCode: Int): Boolean {
        return latestVersionCode > currentVersionCode
    }
}

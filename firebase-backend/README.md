# Firebase Cloud Messaging (FCM) Global Push Notification Backend

This backend provides a secure, production-grade push notification broadcast system for your app.

## Architecture

1. **Client Registration (`AdsearnFirebaseMessagingService.kt`)**:
   - When any user installs and opens the app, Firebase assigns a unique FCM device token.
   - The app automatically subscribes to the app-scoped topic: `adsearn_global_announcements`.
   - The device registration (token, model, Android OS version) is recorded.

2. **Admin Dispatch Console (`AdminDashboardSheet.kt`)**:
   - The authorized admin logs into the Admin Console on their device.
   - Enters the Notification Title and Message with real-time lock-screen preview.
   - Taps **"Send to All Users"**.

3. **Secure Cloud Function (`index.js`)**:
   - The message is dispatched to FCM topic `adsearn_global_announcements`.
   - Firebase Cloud Messaging delivers the push notification to all registered phones worldwide (open, backgrounded, or closed).
   - **Zero Credentials in APK**: Firebase Service Account and private keys reside exclusively on this backend, never exposed in client code.

## Quick Deployment (1 Step)

```bash
cd firebase-backend
npm install
firebase deploy --only functions
```

Once deployed, set the Cloud Function URL as your dispatch endpoint or trigger it directly from the Firebase Console (Cloud Messaging -> Send test message to topic `adsearn_global_announcements`).

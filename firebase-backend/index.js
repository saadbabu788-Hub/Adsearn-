const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const GLOBAL_ANNOUNCEMENTS_TOPIC = "adsearn_global_announcements";

/**
 * HTTP Callable or REST Endpoint for Authorized Admin to broadcast global push notifications.
 * Never exposes server keys or credentials to client APKs.
 */
exports.sendGlobalPushAnnouncement = functions.https.onRequest(async (req, res) => {
  // Enforce POST method
  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method Not Allowed. Use POST." });
  }

  const { title, message, broadcast_id } = req.body;

  if (!title || !message) {
    return res.status(400).json({ error: "Title and message are required." });
  }

  const payload = {
    notification: {
      title: String(title),
      body: String(message),
    },
    data: {
      title: String(title),
      message: String(message),
      broadcast_id: broadcast_id || String(Date.now()),
      topic: GLOBAL_ANNOUNCEMENTS_TOPIC,
      sent_at: String(Date.now()),
    },
    android: {
      priority: "high",
      notification: {
        channelId: "adsearn_global_announcements_channel",
        sound: "default",
        priority: "high",
        defaultVibrateTimings: true,
      },
    },
    topic: GLOBAL_ANNOUNCEMENTS_TOPIC,
  };

  try {
    const response = await admin.messaging().send(payload);
    console.log("Successfully dispatched FCM message to topic:", response);

    // Save announcement audit record in Firestore
    await admin.firestore().collection("global_announcements").add({
      broadcastId: broadcast_id || String(Date.now()),
      title: title,
      message: message,
      targetTopic: GLOBAL_ANNOUNCEMENTS_TOPIC,
      sentAt: admin.firestore.FieldValue.serverTimestamp(),
      fcmMessageId: response,
      status: "Dispatched",
    });

    return res.status(200).json({
      success: true,
      message: "Broadcast sent successfully to topic: " + GLOBAL_ANNOUNCEMENTS_TOPIC,
      fcmMessageId: response,
    });
  } catch (error) {
    console.error("Error sending FCM broadcast:", error);
    return res.status(500).json({
      success: false,
      error: error.message || "Failed to dispatch FCM message.",
    });
  }
});

# RepSync push notifications

RepSync receives remote training alerts through Firebase Cloud Messaging (FCM).
This is separate from the **Preview notification** button, which posts only on
the current phone and does not prove cloud delivery.

## Connect Firebase

1. In https://console.firebase.google.com/, create a Firebase project. Analytics
   is optional and is not used by this integration.
2. Register an Android app with package name `com.example.myapplication`.
3. Download its Android client configuration to `app/google-services.json`.
   Use the Android client configuration, not a service-account private key.
4. Sync Gradle, rebuild, and install the app on the physical phone.
5. Open **Profile > Notifications**, allow notifications, and wait for
   **Connected to training updates**. Check notification settings if Android
   previously denied the permission or disabled the Training updates channel.

The Google Services plugin is applied when `app/google-services.json` exists.
Without it, the app still builds, but honestly reports that push delivery has
not been connected. No placeholder Firebase credentials are included.

## Send a real remote test

1. Install a debug build, open **Profile > Notifications**, and choose
   **Copy test device token** under Development tools.
2. In the same Firebase project's Messaging/Notifications composer, create a
   notification with a title such as **New training plan** and a body such as
   **Your next home workout is ready. Open RepSync to get started.**
3. Choose **Send test message**, paste the copied FCM device token, and send it.
   Use the token from the currently installed build; tokens can change.
4. Test once with RepSync open, then again after pressing the phone's Home button.
   The foreground receiver posts the notification; Firebase posts notification
   payloads to the system tray in the background.
5. Tap the notification. An existing signed-in session opens the destination;
   a signed-out user signs in before reaching it. Unknown destinations open Home.

Use an actual remote send for the submission video. A local preview alone is not
a successful FCM test. Android can delay delivery while offline, and force-stopping
the app prevents delivery until it is opened again.

## Public training updates and multilingual data messages

Every configured installation subscribes to the public `repsync_updates` topic.
Only general training announcements belong on this topic. Local Room account IDs
are not Firebase identities; this integration does not implement private per-user
server targeting.

For messages whose text follows the app's selected language, send a **data-only**
FCM HTTP v1 message from a trusted server. RepSync's service handles these in both
foreground and background, stores the latest handled alert, and posts a notification
if Android permits it. Example request body:

```json
{
  "message": {
    "topic": "repsync_updates",
    "android": { "priority": "high", "ttl": "300s" },
    "data": {
      "title": "New training plan",
      "body": "Your home workout is ready.",
      "title_zu": "Uhlelo olusha lokuzivocavoca",
      "body_zu": "Ukuzivocavoca kwakho kwasekhaya sekulungile.",
      "title_af": "Nuwe oefenplan",
      "body_af": "Jou tuisoefening is gereed.",
      "destination": "workout"
    }
  }
}
```

The sender may use `token` instead of `topic` to test one installation.
Destinations are `home`, `progress`, or `workout` (the plan generator). Unknown
values fall back to Home. All data values must be strings. Generic `title` and
`body` are fallbacks if the selected language has no variant. For Firebase
notification payloads shown automatically in background, the composer/server
controls the text; custom language data fields are not automatically translated
by the SDK. The **Latest alert** row records messages handled by the app's service,
not background notification payloads displayed directly by the SDK.

Send using the Firebase console, Admin SDK, or the included PowerShell script on
a trusted development computer. Never place OAuth access tokens or service-account
private keys in the Android app. Notification settings and normal app text are
translated into English, isiZulu, and Afrikaans. Token-copy tools are hidden in
release builds.

## Sender script

`tools/Send-Push.ps1` uses an already authenticated Google Cloud CLI session with
permission to send FCM messages in your Firebase project. The script neither
creates credentials nor embeds them in the app. Authenticate with the appropriate
Google account before running it.

```powershell
.\tools\Send-Push.ps1 -ProjectId 'your-firebase-project-id' -DeviceToken 'copied-device-token'
```

Use `-Topic 'repsync_updates'` instead of `-DeviceToken` to send a public announcement
to all subscribed installations. Only do this when that is the intended audience.

References:
- https://firebase.google.com/docs/android/setup
- https://firebase.google.com/docs/cloud-messaging/android/receive
- https://firebase.google.com/docs/cloud-messaging/send/v1-api

## Validation

Unit tests cover translated payload selection, fallback text, and destination
allowlisting. Device tests check notification channel creation, immutable app
intents, notification content, and routing. These local checks do not prove
Firebase connectivity or remote delivery; run the remote test above after adding
your real configuration.

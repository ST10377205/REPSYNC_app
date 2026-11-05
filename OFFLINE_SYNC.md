# Offline workout saving and Firestore sync

## What is implemented

- Completing a workout first commits its ID, title, completion time, duration,
  volume, set count and notes to Room on the phone. It does not wait for internet.
- New sessions have a stable UUID. Saving the same summary twice, retrying a
  worker, or restoring an interrupted upload does not add another workout.
- Google-linked profiles own their queued rows by Firebase UID. WorkManager
  uploads pending rows when a network is available, retries transient failures,
  and periodically recovers pending rows after process interruption.
- Successful server acknowledgement marks each row synced. A server read then
  downloads that user's cloud workouts into Room and refreshes the existing Home
  and Progress screens. Profile > Workout sync shows pending count, last successful
  cloud sync, recent records and notes, and a manual retry.
- Previously authenticated sessions reopen without internet. Signing out stops
  that account's scheduled sync. A worker checks both the local profile mapping
  and current Firebase identity before reading or uploading.
- Local email/password accounts can save offline. Cloud sync requires Google
  authentication; connecting the same email to an existing local account still
  requires its RepSync password before the account is linked.

This covers NoSQL document writes and reads as well as offline workout saving
with automatic reconnect sync. It does not synchronize every setting, the exercise
library, or the old global personal-record preference keys.

## Firebase setup

Use project `repsync-5f2b3`. Create a Standard/Native Firestore database named
`(default)`. Choose its location, create it in Production mode, then publish the
included `firestore.rules`. No change to `google-services.json` is needed when
enabling Firestore in this existing Firebase project.

Workouts are stored at:

```text
users/{firebaseUid}/workouts/{workoutId}
```

The parent user document may have no fields; open its `workouts` subcollection.
The rules allow a signed-in user to access only their own path, validate the eight
workout fields, and allow idempotent retries of an immutable completed workout.
No public test-mode rules or local numeric account IDs are used for cloud access.

## Existing data

Room upgrades from version 2 to 3 by adding workout and import-state tables; it
keeps user IDs and Google links. Existing history is imported once for each local
profile with stable IDs. Legacy aggregate duration and any workouts not represented
by a parseable history row are retained in the local metrics baseline. Old history
did not contain per-session duration, set counts or notes, so these are not invented
for imported records. Newly saved workouts contain all supported fields.

The old SharedPreferences history/counters remain a projection for the existing
screens. Room is the source of truth for new saves and cloud downloads. Completed
workouts are immutable in the current app; editing, deletion and bidirectional
delete propagation are not part of this feature.

## Physical phone demonstration

1. Sign in with Google while online and open Profile > Workout sync.
2. Turn off the phone's Wi-Fi and mobile data, or enable airplane mode.
3. Complete a short workout, enter a note and save it. Verify it appears in history
   and Workout sync reports it as saved locally and waiting for connection.
4. Close and reopen RepSync while still offline. The saved record should remain.
5. Restore internet. Let the background worker run, or tap Sync now.
6. Verify the pending count reaches zero and last cloud sync updates.
7. In Firestore's Data tab, open the current user's workouts collection and verify
   the same title, note and completion time. Repeated Sync now must not create
   another document for that workout.

## Verification

Unit tests cover legacy parsing, stable IDs and cloud payload ownership/validation.
Device repository tests use isolated databases/preferences for migrations,
idempotent saves, owner binding, cloud reads, and pending records across reopen.
Live verification tests are opt-in (`live_cloud=true`): they check the actual
signed-in account's server read and rejection of another user's path, then capture
pending Room records while internet is off and verify the same IDs on the server
after reconnection. They use real saved workouts and do not create fake fitness
records.

Install the main and test APKs with `adb install -r` and invoke the instrumentation
runner directly for phone tests that use the signed-in profile. Do not use a test
runner configuration that uninstalls the user's app after completion.

References:
- https://firebase.google.com/docs/firestore/quickstart
- https://firebase.google.com/docs/firestore/manage-data/enable-offline
- https://firebase.google.com/docs/firestore/security/rules-conditions

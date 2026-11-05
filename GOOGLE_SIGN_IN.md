# Google sign-in for RepSync

RepSync uses Android Credential Manager's Google button flow and Firebase
Authentication. Google ID tokens are verified by Firebase before a local app
session is created. The request uses a fresh nonce, and raw tokens are not stored
in Room, app preferences, or logs.

## Firebase setup

Use the existing Firebase project `repsync-5f2b3` and Android app
`com.example.myapplication`.

1. Enable Google under Authentication > Sign-in method. Select the project support
   email and save.
2. Add the debug certificate SHA-1 in Project settings > General > Your apps:
   `09:70:63:2B:CB:B6:47:8A:2D:09:DD:A8:DC:B6:E7:3F:22:87:62:4D`.
3. Download a new `google-services.json` and place it in `app/`. It must contain a
   web OAuth client as well as the Android client for the signing certificate.
4. Build and run the app on a phone with Google Play services.

The release build will need its own signing certificate registered in Firebase;
if Play App Signing is used, register the Play app signing certificate too.

## Account behavior

- A first-time Google user receives a local profile after Firebase authentication.
  Google-only profiles have no local password.
- Returning Google users are matched by their Firebase UID and retain the same
  local user ID. The existing workout, history and personal-record storage keeps
  its original user IDs and keys.
- When the Google email matches an existing local password account, RepSync asks
  for that account's password once before linking it. Linking preserves its ID,
  password hash, display name and workout ownership. A wrong password or Cancel
  does not link the account.
- Local password sign-in continues to work. Google-only accounts use Google for
  sign-in and cannot use the local password-reset screen to create a bypass.
- Sign-out clears the local session, signs out Firebase, and clears Credential
  Manager's app session hint so another Google account can be chosen.

Room upgrades from schema version 1 to 2 by adding a nullable, uniquely indexed
Google UID column. It does not use destructive migration or replace existing
user rows. Local accounts remain local until explicitly linked; this feature does
not upload workout records to a cloud database.

## Physical phone test and submission video

1. Open RepSync's login screen and tap **Sign in with Google**.
2. Choose your Google account and complete any consent prompts yourself.
3. If prompted to keep an existing RepSync account, enter that account's RepSync
   password. Otherwise a new profile is created automatically.
4. Confirm that Home opens and Profile displays the expected name and email.
5. Sign out, then sign in with Google again. Confirm the same profile and workout
   progress are shown.
6. Verify that ordinary email/password login still works for an existing local
   account, and that dismissing the Google picker returns to the login screen.

Automated tests cover the additive database migration, new/returning Google UID
mapping, password-protected linking, account conflict handling, safe password
verification, and the visible Google button. These tests do not substitute for a
real interactive Google sign-in on the phone.

References:
- https://firebase.google.com/docs/auth/android/google-signin
- https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation

# RepSync fitness REST API

RepSync reads the public wger exercise database through its HTTPS JSON REST API. This replaces the unrelated AdviceSlip integration. No wger account, API key, Firebase upgrade, or backend deployment is required for public exercise reads.

Official API documentation: https://wger.readthedocs.io/en/latest/api/api.html

## In the app

1. Open **Exercise Library → Online exercises**. The Home exercise spotlight also opens this screen.
2. Browse exercises, categories and equipment. Search filters the exercises already loaded, including their names, categories and equipment.
3. Use **Load more exercises** for the next page. Each request reads 20 records; entries without an English translation are skipped.
4. Tap an exercise for its instructions, author, licence and source links. The instructions are rendered as plain text, without a WebView. The upstream exercise content is English; the app controls and status messages support English, isiZulu and Afrikaans.
5. Tap **Start exercise** to open a workout containing that exercise. Finish and save through the existing Room/offline-sync workflow.
6. Use **Refresh** to fetch the first page again. Successful pages are saved privately on the phone and reused for 24 hours. When a request fails, the saved version of that page remains available and is labelled as saved results. A page never fetched cannot be loaded offline. Refreshing the first page resets the displayed list; previously downloaded later pages can still be loaded from storage.

## API contract

- Base URL: `https://wger.de/api/v2/`
- GET `exerciseinfo/?limit=20&offset=0&language=2&format=json`
- Pagination: increment offset by 20; the response `next` field indicates whether another page exists. Only the fixed wger endpoint is called; arbitrary response URLs are not followed.
- Exercise source: `https://wger.de/api/v2/exerciseinfo/{id}/`
- Each result includes categories, equipment, translations and licence metadata from the exercise database. The English translation (language 2) is explicitly selected from the expanded translation list.
- Requests have bounded timeouts and use Retrofit's cancellable suspend functions. Activity lifecycle cancellation propagates.
- Atomic page writes preserve the last complete saved response. Failed requests and invalid response envelopes do not overwrite saved data.
- Requests contain no RepSync account details, Google/Firebase tokens, or personal workout data.

The source code is in `FitnessApiService.kt`, `OnlineExerciseRepository.kt` and `OnlineExercisesActivity.kt`. `MainActivity.kt` uses the same repository for its exercise spotlight.

## Demonstration video

With internet enabled, open Online exercises, press Refresh, show the “updated from wger” status, search, open a guide, load another page, and start an exercise. Return to the browser after disconnecting from the internet to show saved catalogue pages. Demonstrate workout offline saving and Firestore synchronization separately.

## Verification

Build with the project's Android toolchain: `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`.
`OnlineExerciseRepositoryTest` covers English selection, plain-text descriptions, attribution, persistent caching, request failure fallback, pagination, corrupt data and cancellation. Its live-server test and `OnlineExercisesUiTest` require the instrumentation argument `live_fitness_api=true`. The UI test checks search, guides and the exercise passed to the workout screen. Install with `adb install -r` to preserve current app data, then run instrumentation directly rather than the Gradle connected-test task.

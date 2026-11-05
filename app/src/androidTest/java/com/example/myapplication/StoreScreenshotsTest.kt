package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.View
import androidx.core.widget.NestedScrollView
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.os.LocaleList
import android.app.LocaleManager
import android.view.PixelCopy
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Explicitly invoked, real-device screenshots. Demo rows are local-only and removed afterward. */
@RunWith(AndroidJUnit4::class)
class StoreScreenshotsTest {
    @Test fun captureAssignmentStoreScreenshots() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("store_screenshots") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val prefs = context.getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
        val oldId = if (prefs.contains("current_user_id")) prefs.getInt("current_user_id", -1) else null
        val oldDark = if (prefs.contains("dark_mode")) prefs.getBoolean("dark_mode", true) else null
        val oldLocales = if(Build.VERSION.SDK_INT >= 33) context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags()
            else AppCompatDelegate.getApplicationLocales().toLanguageTags()
        fun setLocales(tags: String) = instrumentation.runOnMainSync {
            // On API 33+, no Activity delegate exists between scenarios: use the system locale service.
            if(Build.VERSION.SDK_INT >= 33) context.getSystemService(LocaleManager::class.java).applicationLocales=LocaleList.forLanguageTags(tags)
            else AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
        }
        val database = AppDatabase.getDatabase(context)
        val demoId = database.userDao().signup(User(fullName="Athlete", email="store-${UUID.randomUUID()}@example.test", password="")).toInt()
        val directory = File(context.filesDir, "store-screenshots").apply { mkdirs() }
        try {
            prefs.edit().putInt("current_user_id", demoId).putBoolean("dark_mode", true).commit()
            setLocales("en")
            // Warm the public catalogue. The screenshot clearly states if only saved data is available.
            runCatching { OnlineExerciseRepository(context).loadPage() }
            fun <T: Activity> capture(type: Class<T>, name: String, requiredLanguage: String = "en", intent: Intent = Intent(context, type)) {
                ActivityScenario.launch<T>(intent).use { scenario ->
                    if (name == "profile-preferences-review") scenario.onActivity { activity ->
                        val row = activity.findViewById<View>(R.id.btnLanguage)
                        val scroll = generateSequence(row.parent) { it.parent }.filterIsInstance<NestedScrollView>().first()
                        val bounds = Rect()
                        row.getDrawingRect(bounds)
                        scroll.offsetDescendantRectToMyCoords(row, bounds)
                        scroll.scrollTo(0, (bounds.top - 64 * activity.resources.displayMetrics.density).toInt().coerceAtLeast(0))
                    }
                    instrumentation.waitForIdleSync()
                    Thread.sleep(1800)
                    val latch = CountDownLatch(1)
                    var result = -1
                    lateinit var bitmap: Bitmap
                    scenario.onActivity { activity ->
                        assertEquals(requiredLanguage, activity.resources.configuration.locales[0].language)
                        val decor = activity.window.decorView
                        bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
                        PixelCopy.request(activity.window, bitmap, { code -> result=code; latch.countDown() }, Handler(Looper.getMainLooper()))
                    }
                    assertTrue(latch.await(5, TimeUnit.SECONDS))
                    assertEquals(PixelCopy.SUCCESS, result)
                    File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                }
            }
            if (InstrumentationRegistry.getArguments().getString("profile_preferences_only") == "true") {
                capture(ProfileActivity::class.java, "profile-preferences-review")
                return@runBlocking
            }
            capture(MainActivity::class.java, "01-home")
            capture(ExerciseLibraryActivity::class.java, "02-exercise-library")
            capture(OnlineExercisesActivity::class.java, "03-online-exercises")
            capture(WorkoutGeneratorActivity::class.java, "04-plan-generator")
            capture(WorkoutSessionActivity::class.java, "05-workout-session")
            capture(ProgressActivity::class.java, "06-progress")
            setLocales("zu")
            capture(ExerciseLibraryActivity::class.java, "07-isizulu", "zu")
            setLocales("af")
            capture(ExerciseLibraryActivity::class.java, "08-afrikaans", "af")
        } finally {
            setLocales(oldLocales)
            val editor = prefs.edit()
            if(oldId == null) editor.remove("current_user_id") else editor.putInt("current_user_id", oldId)
            if(oldDark == null) editor.remove("dark_mode") else editor.putBoolean("dark_mode", oldDark)
            prefs.all.keys.filter { it.startsWith("user_") && it.endsWith("_$demoId") }.forEach { editor.remove(it) }
            editor.commit()
            database.openHelper.writableDatabase.execSQL("DELETE FROM workouts WHERE localUserId = ?", arrayOf(demoId))
            database.openHelper.writableDatabase.execSQL("DELETE FROM workout_import_state WHERE localUserId = ?", arrayOf(demoId))
            database.openHelper.writableDatabase.execSQL("DELETE FROM users WHERE id = ?", arrayOf(demoId))
            WorkoutSyncScheduler.enqueueCurrent(context)
            assertNull(database.userDao().getUserById(demoId))
            assertEquals(oldId, if(prefs.contains("current_user_id")) prefs.getInt("current_user_id", -1) else null)
        }
    }
}

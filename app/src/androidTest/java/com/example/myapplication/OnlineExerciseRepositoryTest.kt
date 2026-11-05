package com.example.myapplication

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class OnlineExerciseRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun sample() = ApiExercise(9, ApiLabel(10, "Abs"), listOf(ApiLabel(10, "Kettlebell")),
        listOf(ApiTranslation(1, "German", "Falsch"), ApiTranslation(2, "Swing", "<p>Hinge &amp; stand.</p>", 2, "Author")),
        ApiLicense(2, "CC BY-SA 4", "https://creativecommons.org/licenses/by-sa/4.0/"))
    private class FakeApi(var page: ExercisePage) : FitnessApiService {
        var failure: Exception? = null
        var calls = 0
        val offsets = mutableListOf<Int>()
        override suspend fun getExercises(limit: Int, offset: Int, language: Int, format: String): ExercisePage {
            calls++
            offsets += offset
            failure?.let { throw it }
            return page
        }
    }
    private fun directory() = File(context.cacheDir, "catalogue-test-${UUID.randomUUID()}")

    @Test fun mapsEnglishHtmlAndLicenseWithoutUsingAnotherLanguage() {
        val exercise = OnlineExerciseRepository.mapExercise(sample())!!
        assertEquals("Swing", exercise.name)
        assertEquals("Hinge & stand.", exercise.instructions)
        assertEquals("Author", exercise.author)
        assertEquals("CC BY-SA 4", exercise.license)
        assertNull(OnlineExerciseRepository.mapExercise(sample().copy(translations = listOf(ApiTranslation(1, "German")))))
        assertNull(OnlineExerciseRepository.mapExercise(sample().copy(id = 0)))
    }

    @Test fun savesPagesAndReturnsPersistedDataWhenRefreshFails() = runBlocking {
        val directory = directory()
        try {
            val api = FakeApi(ExercisePage(40, "https://wger.de/api/v2/exerciseinfo/?offset=20", listOf(sample())))
            val repository = OnlineExerciseRepository(context, api, directory)
            val live = repository.loadPage()
            assertFalse(live.saved)
            assertTrue(live.hasMore)
            assertEquals(9, live.exercises.single().id)
            val cached = OnlineExerciseRepository(context, api, directory).loadPage()
            assertTrue(cached.saved)
            assertEquals(1, api.calls)
            api.failure = IOException("offline")
            val offline = repository.loadPage(refresh = true)
            assertTrue(offline.saved)
            assertEquals(live.exercises, offline.exercises)
            api.failure = null
            repository.loadPage(offset = 20)
            assertEquals(listOf(0, 0, 20), api.offsets)
            assertTrue(File(directory, "page-20.json").exists())
        } finally { directory.deleteRecursively() }
    }

    @Test fun corruptCacheAndMalformedResponseDoNotReportSuccess() = runBlocking {
        val directory = directory()
        try {
            directory.mkdirs()
            File(directory, "page-0.json").writeText("not json")
            val api = FakeApi(ExercisePage(results = null))
            try {
                OnlineExerciseRepository(context, api, directory).loadPage()
                fail("Invalid data must fail")
            } catch (_: IOException) { }
            api.failure = CancellationException("screen closed")
            try {
                OnlineExerciseRepository(context, api, directory).loadPage()
                fail("Cancellation must propagate")
            } catch (_: CancellationException) { }
        } finally { directory.deleteRecursively() }
    }

    @Test fun liveWgerReturnsUsableExercisesAndCachesThem() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("live_fitness_api") == "true")
        val directory = directory()
        try {
            val repository = OnlineExerciseRepository(context, directory = directory)
            val live = repository.loadPage(refresh = true)
            assertFalse(live.saved)
            assertTrue(live.exercises.isNotEmpty())
            assertTrue(live.exercises.all { it.id > 0 && it.name.isNotBlank() })
            assertTrue(live.exercises.any { it.instructions.isNotBlank() })
            assertTrue(repository.loadPage().saved)
        } finally { directory.deleteRecursively() }
    }
}

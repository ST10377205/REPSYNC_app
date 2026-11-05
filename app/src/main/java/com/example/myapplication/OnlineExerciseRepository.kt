package com.example.myapplication

import android.content.Context
import androidx.core.text.HtmlCompat
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

data class ExercisePage(val count: Int = 0, val next: String? = null, val results: List<ApiExercise>? = null)
data class ApiLabel(val id: Int = 0, val name: String? = null)
data class ApiLicense(val id: Int = 0, @SerializedName("full_name") val name: String? = null, val url: String? = null)
data class ApiTranslation(
    val language: Int = 0, val name: String? = null, val description: String? = null,
    val license: Int = 0, @SerializedName("license_author") val author: String? = null
)
data class ApiExercise(
    val id: Int = 0, val category: ApiLabel? = null, val equipment: List<ApiLabel>? = null,
    val translations: List<ApiTranslation>? = null, val license: ApiLicense? = null
)
data class OnlineExercise(
    val id: Int, val name: String, val category: String, val equipment: String,
    val instructions: String, val author: String, val license: String, val licenseUrl: String
) {
    val sourceUrl get() = "https://wger.de/api/v2/exerciseinfo/$id/"
}
data class ExercisePageResult(val exercises: List<OnlineExercise>, val hasMore: Boolean, val saved: Boolean)

/** Only public catalogue data is cached. No RepSync identity or workout data is sent to wger. */
class OnlineExerciseRepository(
    context: Context,
    private val api: FitnessApiService = FitnessApiService.create(),
    private val directory: File = File(context.applicationContext.filesDir, "exercise-catalogue-v1")
) {
    private val gson = Gson()

    suspend fun loadPage(offset: Int = 0, refresh: Boolean = false): ExercisePageResult = withContext(Dispatchers.IO) {
        require(offset >= 0)
        val file = File(directory, "page-$offset.json")
        val cached = synchronized(cacheLock) {
            runCatching { gson.fromJson(file.readText(), ExercisePage::class.java) }
                .getOrNull()?.takeIf { it.results != null && it.results.size <= FitnessApiService.PAGE_SIZE }
        }
        val fresh = file.exists() && System.currentTimeMillis() - file.lastModified() in 0..86_400_000L
        if (!refresh && fresh && cached != null) return@withContext cached.toResult(true)
        try {
            val page = api.getExercises(offset = offset)
            if (page.results == null || page.results.size > FitnessApiService.PAGE_SIZE || page.count < 0) {
                throw IOException("Invalid exercise catalogue response")
            }
            // Save a complete response atomically; a failed fetch never replaces the last good page.
            synchronized(cacheLock) { runCatching {
                directory.mkdirs()
                val atomicFile = android.util.AtomicFile(file)
                var stream: java.io.FileOutputStream? = null
                try {
                    stream = atomicFile.startWrite()
                    stream.write(gson.toJson(page).toByteArray(Charsets.UTF_8))
                    atomicFile.finishWrite(stream)
                } catch (error: Exception) {
                    if (stream != null) atomicFile.failWrite(stream)
                    throw error
                }
            } }
            page.toResult(false)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            cached?.toResult(true) ?: throw error
        }
    }

    private fun ExercisePage.toResult(saved: Boolean) = ExercisePageResult(
        results.orEmpty().mapNotNull(::mapExercise), next != null, saved
    )

    companion object {
        private val cacheLock = Any()
        internal fun mapExercise(exercise: ApiExercise): OnlineExercise? {
            val translation = exercise.translations?.firstOrNull { it.language == 2 && !it.name.isNullOrBlank() }
                ?: return null
            if (exercise.id <= 0) return null
            fun plain(value: String?) = HtmlCompat.fromHtml(value.orEmpty(), HtmlCompat.FROM_HTML_MODE_LEGACY)
                .toString().trim()
            val sameLicense = translation.license == exercise.license?.id
            return OnlineExercise(
                exercise.id, plain(translation.name), plain(exercise.category?.name),
                exercise.equipment.orEmpty().mapNotNull { it.name }.joinToString(" · "),
                plain(translation.description), translation.author.orEmpty(),
                if (sameLicense) exercise.license?.name.orEmpty() else "wger #${translation.license}",
                if (sameLicense) exercise.license?.url.orEmpty() else "https://wger.de/api/v2/license/${translation.license}/"
            )
        }
    }
}

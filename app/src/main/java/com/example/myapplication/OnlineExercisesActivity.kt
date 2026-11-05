package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.util.Linkify
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class OnlineExercisesActivity : BaseActivity() {
    private val repository by lazy { OnlineExerciseRepository(this) }
    private val exercises = linkedMapOf<Int, OnlineExercise>()
    private var offset = 0
    private var hasMore = false
    private var loading = false
    private var query = ""
    private var status = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_online_exercises)
        applySystemBarInsets(R.id.onlineExercisesMain)
        setupToolbarNavigation()
        findViewById<EditText>(R.id.etOnlineSearch).addTextChangedListener {
            query = it.toString().trim()
            render()
        }
        findViewById<MaterialButton>(R.id.btnOnlineRefresh).setOnClickListener { load(refresh = true) }
        findViewById<MaterialButton>(R.id.btnOnlineMore).setOnClickListener { load() }
        load()
    }

    private fun load(refresh: Boolean = false) {
        if (loading) return
        loading = true
        status = getString(R.string.online_loading)
        render()
        lifecycleScope.launch {
            try {
                val requestedOffset = if (refresh) 0 else offset
                val page = repository.loadPage(requestedOffset, refresh)
                if (refresh) exercises.clear()
                page.exercises.forEach { exercises[it.id] = it }
                offset = requestedOffset + FitnessApiService.PAGE_SIZE
                hasMore = page.hasMore
                status = getString(if (page.saved) R.string.online_saved else R.string.online_live, exercises.size)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                status = getString(R.string.online_error)
            } finally {
                loading = false
                render()
            }
        }
    }

    private fun render() {
        findViewById<TextView>(R.id.tvOnlineStatus).text = status
        findViewById<MaterialButton>(R.id.btnOnlineRefresh).isEnabled = !loading
        findViewById<MaterialButton>(R.id.btnOnlineMore).apply {
            isEnabled = !loading
            visibility = if (hasMore) View.VISIBLE else View.GONE
        }
        val container = findViewById<LinearLayout>(R.id.llOnlineExercises)
        container.removeAllViews()
        val matching = exercises.values.filter {
            "${it.name} ${it.category} ${it.equipment}".contains(query, ignoreCase = true)
        }
        matching.forEach { exercise ->
            container.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = listOf(exercise.name, listOf(exercise.category, exercise.equipment)
                    .filter { it.isNotBlank() }.joinToString(" · ")).joinToString("\n")
                isAllCaps = false
                minHeight = (72 * resources.displayMetrics.density).toInt()
                setTextColor(getColor(R.color.text_primary))
                setOnClickListener { showExercise(exercise) }
            })
        }
        if (!loading && matching.isEmpty()) container.addView(TextView(this).apply {
            text = getString(R.string.online_no_results)
            setTextColor(getColor(R.color.text_secondary))
            setPadding(0, 24, 0, 24)
        })
    }

    private fun showExercise(exercise: OnlineExercise) {
        val message = listOf(
            exercise.instructions.ifBlank { getString(R.string.online_no_guide) },
            getString(R.string.online_attribution, exercise.author, exercise.license),
            exercise.licenseUrl, exercise.sourceUrl
        ).filter { it.isNotBlank() }.joinToString("\n\n")
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(exercise.name)
            .setMessage(message)
            .setPositiveButton(R.string.online_start) { _, _ ->
                startActivity(Intent(this, WorkoutSessionActivity::class.java).apply {
                    putStringArrayListExtra("EXERCISE_NAMES", arrayListOf(exercise.name))
                    putStringArrayListExtra("EXERCISE_CATEGORIES", arrayListOf(exercise.category))
                    putExtra("ROUTINE_TITLE", exercise.name)
                })
            }
            .setNegativeButton(R.string.ui_got_it, null).show()
        dialog.findViewById<TextView>(android.R.id.message)?.let { Linkify.addLinks(it, Linkify.WEB_URLS) }
    }
}

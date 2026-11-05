package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class WorkoutSyncActivity : BaseActivity() {
    private var userId = -1
    private var googleUid: String? = null
    private var rows: List<Workout> = emptyList()
    private var observer: Job? = null
    private val state by lazy { getSharedPreferences("WorkoutSyncPrefs", Context.MODE_PRIVATE) }
    private val connectivity by lazy { getSystemService(ConnectivityManager::class.java) }
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }
    private var networkRegistered = false
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { runOnUiThread { refresh() } }
        override fun onLost(network: Network) { runOnUiThread { refresh() } }
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) { runOnUiThread { refresh() } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_sync)
        applySystemBarInsets(R.id.workoutSyncMain)
        setupToolbarNavigation()
        userId = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE).getInt("current_user_id", -1)
        if (userId < 1) { finish(); return }
        findViewById<MaterialButton>(R.id.btnSyncNow).setOnClickListener { WorkoutSyncScheduler.enqueueCurrent(this) }
        findViewById<MaterialButton>(R.id.btnSyncGoogle).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java).putExtra("show_login", true))
        }
    }

    override fun onStart() {
        super.onStart()
        if (userId < 1) return
        if (getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE).getInt("current_user_id", -1) != userId) { finish(); return }
        state.registerOnSharedPreferenceChangeListener(preferenceListener)
        connectivity.registerDefaultNetworkCallback(networkCallback)
        networkRegistered = true
        observer = lifecycleScope.launch {
            val database = AppDatabase.getDatabase(this@WorkoutSyncActivity)
            googleUid = database.userDao().getUserById(userId)?.googleUid
            val repository = WorkoutRepository(this@WorkoutSyncActivity)
            repository.ensureLegacyImported(userId)
            repository.observe(userId).collect {
                rows = it
                refresh()
            }
        }
        WorkoutSyncScheduler.enqueueCurrent(this)
    }

    override fun onStop() {
        observer?.cancel()
        state.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        if (networkRegistered) connectivity.unregisterNetworkCallback(networkCallback)
        networkRegistered = false
        super.onStop()
    }

    private fun refresh() {
        if (isFinishing || isDestroyed || userId < 1) return
        val currentUid = if (PushNotifications.isConfigured()) FirebaseAuth.getInstance().currentUser?.uid else null
        val authenticated = googleUid != null && currentUid == googleUid &&
            getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE).getString("firebase_uid", null) == googleUid
        val pending = rows.count { it.syncState != WorkoutSyncState.SYNCED }
        val online = connectivity.getNetworkCapabilities(connectivity.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val status = state.getString("status_$userId", "")
        val message = when {
            !authenticated -> getString(R.string.sync_local_only)
            !online -> getString(R.string.sync_offline)
            status == "SETUP" -> getString(R.string.sync_setup_error)
            status == "RETRY" -> getString(R.string.sync_retry_error)
            status == "RUNNING" -> getString(R.string.sync_running)
            pending > 0 -> getString(R.string.sync_pending, pending)
            state.getLong("last_success_$userId", 0) > 0 -> getString(R.string.sync_complete)
            else -> getString(R.string.sync_ready)
        }
        findViewById<TextView>(R.id.tvSyncStatus).text = message
        findViewById<TextView>(R.id.tvSyncCount).text = getString(R.string.sync_count, rows.size, pending)
        val timestamp = state.getLong("last_success_$userId", 0)
        findViewById<TextView>(R.id.tvLastSync).apply {
            visibility = if (timestamp > 0) View.VISIBLE else View.GONE
            if (timestamp > 0) text = getString(R.string.sync_last, DateFormat.getDateTimeInstance().format(Date(timestamp)))
        }
        findViewById<View>(R.id.btnSyncGoogle).visibility = if (authenticated) View.GONE else View.VISIBLE
        findViewById<MaterialButton>(R.id.btnSyncNow).isEnabled = authenticated
        val list = findViewById<LinearLayout>(R.id.llSyncWorkouts)
        list.removeAllViews()
        for (row in rows.take(10)) {
            list.addView(TextView(this).apply {
                text = localizedText(row.title) + "\n" + DateFormat.getDateTimeInstance().format(Date(row.completedAt)) +
                    " • " + row.totalVolume.toLong() + " kg" + if (row.notes.isBlank()) "" else "\n" + row.notes
                setTextColor(getColor(R.color.text_primary))
                textSize = 15f
                val padding = (12 * resources.displayMetrics.density).toInt()
                setPadding(0, padding, 0, padding)
            })
        }
    }
}

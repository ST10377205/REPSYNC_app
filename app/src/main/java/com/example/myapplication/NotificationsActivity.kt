package com.example.myapplication

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class NotificationsActivity : BaseActivity() {
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        updatePermissionState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)
        applySystemBarInsets(R.id.notificationsMain)
        setupToolbarNavigation()

        findViewById<MaterialButton>(R.id.btnAllowNotifications).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !PushNotifications.notificationsAllowed(this)) {
                permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        findViewById<MaterialButton>(R.id.btnNotificationSystemSettings).setOnClickListener {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$packageName"))
            }
            startActivity(intent)
        }
        findViewById<MaterialButton>(R.id.btnPreviewNotification).setOnClickListener {
            val shown = PushNotifications.show(this, NotificationContent(
                getString(R.string.push_preview_title), getString(R.string.push_preview_body)
            ), "local_preview")
            if (!shown) Toast.makeText(this, R.string.push_preview_failed, Toast.LENGTH_LONG).show()
        }
        findViewById<MaterialButton>(R.id.btnRetryPush).setOnClickListener { connect() }
        findViewById<View>(R.id.pushDeveloperTools).visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE
        findViewById<MaterialButton>(R.id.btnCopyPushToken).setOnClickListener {
            val token = PushNotifications.token(this)
            if (token.isNullOrBlank()) {
                Toast.makeText(this, R.string.push_token_unavailable, Toast.LENGTH_LONG).show()
            } else {
                getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("FCM test token", token))
                Toast.makeText(this, R.string.push_token_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionState()
        connect()
    }

    private fun updatePermissionState() {
        val allowed = PushNotifications.notificationsAllowed(this)
        findViewById<TextView>(R.id.tvNotificationPermission).setText(if (allowed) R.string.push_enabled else R.string.push_disabled)
        findViewById<View>(R.id.btnAllowNotifications).visibility =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) View.VISIBLE else View.GONE
        val title = PushNotifications.lastReceivedTitle(this)
        findViewById<TextView>(R.id.tvLastPush).text = if (title == null) getString(R.string.push_no_messages)
            else getString(R.string.push_last_received, title)
    }

    private fun connect() {
        val state = findViewById<TextView>(R.id.tvPushConnection)
        if (!PushNotifications.isConfigured()) {
            state.setText(R.string.push_unconfigured)
            return
        }
        state.setText(R.string.push_connecting)
        PushNotifications.connect(this) { connected ->
            if (!isFinishing && !isDestroyed) state.setText(if (connected) R.string.push_connected else R.string.push_retry)
        }
    }
}

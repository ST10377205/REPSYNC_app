package com.example.myapplication

import android.app.Application

class RepSyncApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PushNotifications.createChannel(this)
        PushNotifications.connect(this)
        WorkoutSyncScheduler.enqueueCurrent(this)
    }
}

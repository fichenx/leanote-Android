package com.leanote.next

import android.app.Application
import com.leanote.next.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LeanoteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SyncScheduler.schedulePeriodic(this)
    }
}

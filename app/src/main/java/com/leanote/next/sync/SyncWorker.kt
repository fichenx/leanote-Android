package com.leanote.next.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.leanote.next.data.prefs.AccountStore
import com.leanote.next.data.prefs.SettingsStore
import com.leanote.next.data.sync.SyncEngine
import com.leanote.next.widget.WidgetUpdater
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncWorkerEntryPoint::class.java,
        )
        val settings = entryPoint.settingsStore().settings.first()
        if (!settings.autoSync && !inputData.getBoolean(KEY_FORCE, false)) {
            return Result.success()
        }
        if (entryPoint.accountStore().current() == null) {
            return Result.success()
        }
        val summary = entryPoint.syncEngine().sync()
        WidgetUpdater.refresh(applicationContext)
        return if (summary.success) {
            Result.success(
                workDataOf(
                    "pushed" to summary.pushed,
                    "downloaded" to summary.downloaded,
                    "conflicts" to summary.conflicts,
                )
            )
        } else {
            Result.retry()
        }
    }

    companion object {
        const val KEY_FORCE = "force"
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface SyncWorkerEntryPoint {
    fun syncEngine(): SyncEngine
    fun accountStore(): AccountStore
    fun settingsStore(): SettingsStore
}

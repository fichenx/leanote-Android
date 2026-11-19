package com.leanote.next.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.action.clickable
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.leanote.next.MainActivity
import com.leanote.next.data.local.NoteEntity
import com.leanote.next.data.prefs.AccountStore
import com.leanote.next.data.repo.NoteRepository
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class RecentNotesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val notes = loadRecent(context)
        provideContent {
            GlanceTheme {
                WidgetContent(context, notes)
            }
        }
    }
}

@Composable
private fun WidgetContent(context: Context, notes: List<NoteEntity>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(14.dp),
    ) {
        Text(
            text = "最近笔记",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        if (notes.isEmpty()) {
            Text(
                text = "还没有笔记，打开应用开始记录",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                modifier = GlanceModifier.padding(top = 10.dp),
            )
        } else {
            notes.take(4).forEach { note ->
                Text(
                    text = note.title.ifBlank { "无标题笔记" },
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 14.sp,
                    ),
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable(
                            actionStartActivity(
                                Intent(context, MainActivity::class.java).apply {
                                    putExtra(MainActivity.EXTRA_NOTE_ID, note.localId)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                }
                            )
                        ),
                )
            }
        }
    }
}

class RecentNotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecentNotesWidget()
}

object WidgetUpdater {
    /** 同步完成后刷新桌面小组件。 */
    fun refresh(context: Context) {
        runCatching {
            runBlocking(Dispatchers.IO) {
                RecentNotesWidget().updateAll(context.applicationContext)
            }
        }
    }
}

private suspend fun loadRecent(context: Context): List<NoteEntity> =
    withContext(Dispatchers.IO) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        )
        val account = entryPoint.accountStore().current() ?: return@withContext emptyList()
        entryPoint.noteRepository().recent(account.id, 4)
    }

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface WidgetEntryPoint {
    fun accountStore(): AccountStore
    fun noteRepository(): NoteRepository
}

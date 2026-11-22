package com.leanote.next.data.sync

import com.leanote.next.data.local.NoteDao
import com.leanote.next.data.local.NotebookDao
import com.leanote.next.data.local.NotebookEntity
import com.leanote.next.data.model.Account
import com.leanote.next.data.model.ApiException
import com.leanote.next.data.prefs.AccountStore
import com.leanote.next.data.remote.ApiClient
import com.leanote.next.data.repo.ImageRepository
import com.leanote.next.data.repo.NoteRepository
import com.leanote.next.data.repo.NotebookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class SyncSummary(
    val success: Boolean,
    val message: String = "",
    val pushed: Int = 0,
    val conflicts: Int = 0,
    val downloaded: Int = 0,
    val failed: Int = 0,
)

@Singleton
class SyncEngine @Inject constructor(
    private val apiClient: ApiClient,
    private val accountStore: AccountStore,
    private val noteDao: NoteDao,
    private val notebookDao: NotebookDao,
    private val noteRepository: NoteRepository,
    private val notebookRepository: NotebookRepository,
    private val imageRepository: ImageRepository,
) {

    suspend fun sync(): SyncSummary = withContext(Dispatchers.IO) {
        val account = accountStore.current()
            ?: return@withContext SyncSummary(false, "请先登录 Leanote 账户")

        try {
            val pushResult = noteRepository.pushAllDirty(account)
            val freshAccount = accountStore.current() ?: account

            val (notebookUsn, _) = pullNotebooks(freshAccount)
            val (noteUsn, downloaded) = pullNotes(freshAccount)

            if (notebookDao.getAll(freshAccount.id).isEmpty()) {
                runCatching { notebookRepository.create(freshAccount, "我的笔记") }
            }

            accountStore.updateUsn(
                accountId = freshAccount.id,
                noteUsn = noteUsn,
                notebookUsn = notebookUsn,
                lastSyncAt = System.currentTimeMillis(),
            )
            imageRepository.invalidateThumbnailCache()

            SyncSummary(
                success = true,
                pushed = pushResult[0],
                conflicts = pushResult[1],
                failed = pushResult[2],
                downloaded = downloaded,
            )
        } catch (e: ApiException) {
            SyncSummary(false, e.message ?: "同步失败")
        } catch (e: IOException) {
            SyncSummary(false, "网络不可用，已保留本地修改")
        } catch (e: Exception) {
            SyncSummary(false, "同步失败：${e.message ?: "未知错误"}")
        }
    }

    private suspend fun pullNotebooks(account: Account): Pair<Int, Int> {
        val api = apiClient.api(account.host)
        var usn = account.notebookUsn
        var count = 0
        while (true) {
            val page = api.syncNotebooks(usn, PAGE_SIZE, account.token)
            if (page.isEmpty()) break
            page.forEach { dto ->
                usn = maxOf(usn, dto.usn)
                val existing = notebookDao.getByServerId(account.id, dto.notebookId)
                if (dto.isDeleted) {
                    existing?.let { notebookDao.delete(it) }
                } else {
                    notebookDao.upsert(
                        NotebookEntity(
                            localId = existing?.localId ?: 0,
                            accountId = account.id,
                            serverId = dto.notebookId,
                            parentServerId = dto.parentNotebookId ?: "",
                            title = dto.title,
                            seq = dto.seq,
                            usn = dto.usn,
                        )
                    )
                    count++
                }
            }
            if (page.size < PAGE_SIZE) break
        }
        return usn to count
    }

    private suspend fun pullNotes(account: Account): Pair<Int, Int> {
        val api = apiClient.api(account.host)
        var usn = account.noteUsn
        var count = 0
        while (true) {
            val page = api.syncNotes(usn, PAGE_SIZE, account.token)
            if (page.isEmpty()) break
            page.forEach { meta ->
                usn = maxOf(usn, meta.usn)
                val local = noteDao.getByServerId(account.id, meta.noteId)
                if (meta.isDeleted) {
                    local?.let { noteDao.delete(it) }
                    return@forEach
                }
                if (local != null && local.usn >= meta.usn && local.isTrash == meta.isTrash) {
                    return@forEach
                }
                val full = api.noteAndContent(meta.noteId, account.token)
                if (!full.ok) return@forEach
                noteRepository.applyRemoteWithConflict(full, account.id)
                count++
            }
            if (page.size < PAGE_SIZE) break
        }
        return usn to count
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}

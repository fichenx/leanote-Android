package com.leanote.next.data.repo

import com.leanote.next.data.local.NotebookDao
import com.leanote.next.data.local.NotebookEntity
import com.leanote.next.data.model.Account
import com.leanote.next.data.model.ApiException
import com.leanote.next.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotebookRepository @Inject constructor(
    private val notebookDao: NotebookDao,
    private val apiClient: ApiClient,
) {

    fun observe(accountId: String): Flow<List<NotebookEntity>> = notebookDao.observe(accountId)

    suspend fun getAll(accountId: String): List<NotebookEntity> = notebookDao.getAll(accountId)

    suspend fun create(account: Account, title: String, parentServerId: String? = null): NotebookEntity =
        withContext(Dispatchers.IO) {
            val api = apiClient.api(account.host)
            val response = runCatching { api.addNotebook(title, parentServerId, account.token) }
                .getOrElse { throw ApiException("创建笔记本失败：${it.message}") }
            if (!response.ok) throw ApiException(response.msg ?: "创建笔记本失败")
            val entity = NotebookEntity(
                accountId = account.id,
                serverId = response.notebookId,
                parentServerId = response.parentNotebookId ?: "",
                title = response.title,
                seq = response.seq,
                usn = response.usn,
            )
            val localId = notebookDao.upsert(entity)
            entity.copy(localId = if (localId > 0) localId else 0)
        }

    suspend fun rename(account: Account, notebook: NotebookEntity, newTitle: String) = withContext(Dispatchers.IO) {
        val api = apiClient.api(account.host)
        val response = runCatching {
            api.updateNotebook(notebook.serverId, newTitle, notebook.parentServerId.ifBlank { null }, notebook.seq, notebook.usn, account.token)
        }.getOrElse { throw ApiException("重命名失败：${it.message}") }
        if (!response.ok) throw ApiException(response.msg ?: "重命名失败")
        notebookDao.upsert(
            notebook.copy(title = newTitle, usn = response.usn, seq = response.seq)
        )
    }

    suspend fun delete(account: Account, notebook: NotebookEntity) = withContext(Dispatchers.IO) {
        val api = apiClient.api(account.host)
        val response = runCatching { api.deleteNotebook(notebook.serverId, notebook.usn, account.token) }
            .getOrElse { throw ApiException("删除失败：${it.message}") }
        if (!response.ok) throw ApiException(response.msg ?: "删除失败")
        notebookDao.delete(notebook)
    }

    /** 首次使用时确保至少有一个笔记本可用。 */
    suspend fun ensureDefault(account: Account): NotebookEntity? = withContext(Dispatchers.IO) {
        val existing = notebookDao.getAll(account.id).firstOrNull { it.parentServerId.isBlank() }
        if (existing != null) return@withContext existing
        runCatching { create(account, "我的笔记") }.getOrNull()
    }
}

package com.leanote.next.data.repo

import com.leanote.next.data.local.NoteDao
import com.leanote.next.data.local.NoteEntity
import com.leanote.next.data.model.Account
import com.leanote.next.data.model.ApiException
import com.leanote.next.data.remote.ApiClient
import com.leanote.next.data.remote.dto.NoteDto
import com.leanote.next.data.remote.dto.NoteFileDto
import com.leanote.next.util.ImageRefs
import com.leanote.next.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class PushResult { SUCCESS, CONFLICT, SKIPPED }

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val imageRepo: ImageRepository,
    private val apiClient: ApiClient,
) {

    fun observeNotes(accountId: String, notebookId: String = "", trash: Boolean = false) =
        noteDao.observe(accountId, notebookId, trash)

    fun observeNote(localId: Long): Flow<NoteEntity?> = noteDao.observeById(localId)

    fun observeTagRows(accountId: String): Flow<List<String>> = noteDao.observeTagRows(accountId)

    fun observeCount(accountId: String): Flow<Int> = noteDao.observeCount(accountId)

    suspend fun get(localId: Long): NoteEntity? = noteDao.getById(localId)

    suspend fun recent(accountId: String, limit: Int = 6): List<NoteEntity> =
        noteDao.recent(accountId, limit)

    suspend fun create(
        accountId: String,
        notebookServerId: String,
        markdown: Boolean,
        title: String = "",
        content: String = "",
        tags: String = "",
        isBlog: Boolean = false,
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        noteDao.insert(
            NoteEntity(
                accountId = accountId,
                notebookServerId = notebookServerId,
                title = title,
                content = content,
                isMarkdown = markdown,
                tags = tags,
                isBlog = isBlog,
                createdTime = now,
                updatedTime = now,
                isDirty = true,
            )
        )
    }

    suspend fun saveLocal(
        localId: Long,
        title: String,
        content: String,
        tags: String,
        isMarkdown: Boolean,
        isBlog: Boolean,
        notebookServerId: String,
    ) = withContext(Dispatchers.IO) {
        val note = noteDao.getById(localId) ?: return@withContext
        noteDao.upsert(
            note.copy(
                title = title,
                content = content,
                tags = tags,
                isMarkdown = isMarkdown,
                isBlog = isBlog,
                notebookServerId = notebookServerId.ifBlank { note.notebookServerId },
                isDirty = true,
                updatedTime = System.currentTimeMillis(),
            )
        )
    }

    suspend fun setTrash(localId: Long, trash: Boolean) = withContext(Dispatchers.IO) {
        noteDao.setTrash(localId, trash, System.currentTimeMillis())
    }

    suspend fun deleteForever(account: Account, note: NoteEntity) = withContext(Dispatchers.IO) {
        if (note.serverId.isNotBlank()) {
            val response = runCatching {
                apiClient.api(account.host).deleteTrash(note.serverId, note.usn, account.token)
            }.getOrElse { throw ApiException("删除失败：${it.message ?: "网络错误"}") }
            if (!response.ok) throw ApiException(response.msg ?: "删除失败")
        }
        imageRepo.deleteOfNote(note.localId)
        noteDao.delete(note)
    }

    suspend fun push(localId: Long, account: Account): PushResult = withContext(Dispatchers.IO) {
        val note = noteDao.getById(localId) ?: return@withContext PushResult.SKIPPED
        if (note.notebookServerId.isBlank()) return@withContext PushResult.SKIPPED
        val api = apiClient.api(account.host)
        val isNew = note.serverId.isBlank()

        if (!isNew) {
            val remote = runCatching { api.noteAndContent(note.serverId, account.token) }.getOrNull()
            if (remote != null && remote.ok && remote.usn != note.usn) {
                // 服务器已有更新：本地版本另存为冲突副本，再用服务器版本覆盖当前记录。
                val now = System.currentTimeMillis()
                noteDao.insert(
                    note.copy(
                        localId = 0,
                        serverId = "",
                        usn = 0,
                        title = (note.title.ifBlank { "无标题笔记" }) + "（冲突副本）",
                        isDirty = true,
                        createdTime = now,
                        updatedTime = now,
                    )
                )
                noteDao.upsert(remote.toEntity(account.id, note.localId))
                return@withContext PushResult.CONFLICT
            }
        }

        val fields = LinkedHashMap<String, RequestBody>()
        val files = ArrayList<MultipartBody.Part>()

        val localIds = ImageRefs.localIds(note.content)
        val images = imageRepo.listByLocalIds(localIds)
        val uploaded = images.filter { !it.serverId.isNullOrBlank() }
        var content = ImageRefs.replaceLocalIds(
            note.content,
            uploaded.associate { it.localId to apiClient.imageUrl(account.host, it.serverId!!) },
        )
        val pending = images.filter { it.serverId.isNullOrBlank() }

        fields["NotebookId"] = note.notebookServerId.toPart()
        fields["Title"] = note.title.toPart()
        fields["Content"] = content.toPart()
        fields["IsMarkdown"] = (if (note.isMarkdown) "1" else "0").toPart()
        fields["IsBlog"] = (if (note.isBlog) "1" else "0").toPart()
        fields["IsTrash"] = (if (note.isTrash) "1" else "0").toPart()

        val tags = note.tagList
        tags.forEachIndexed { index, tag -> fields["Tags[$index]"] = tag.toPart() }

        if (isNew) {
            val created = if (note.createdTime > 0) note.createdTime else System.currentTimeMillis()
            fields["CreatedTime"] = TimeUtils.toServerTime(created).toPart()
            fields["UpdatedTime"] = TimeUtils.toServerTime(System.currentTimeMillis()).toPart()
        } else {
            fields["NoteId"] = note.serverId.toPart()
            fields["Usn"] = note.usn.toString().toPart()
        }

        pending.forEachIndexed { index, image ->
            val file = File(image.localPath)
            if (!file.isFile) return@forEachIndexed
            fields["Files[$index][LocalFileId]"] = image.localId.toPart()
            fields["Files[$index][IsAttach]"] = "0".toPart()
            fields["Files[$index][FileId]"] = "".toPart()
            fields["Files[$index][HasBody]"] = "1".toPart()
            val body = file.asRequestBody(image.mimeType.toMediaType())
            files.add(MultipartBody.Part.createFormData("FileDatas[${image.localId}]", file.name, body))
        }

        val response = runCatching {
            if (isNew) api.addNote(fields, files, account.token)
            else api.updateNote(fields, files, account.token)
        }.getOrElse { throw ApiException("同步失败：${it.message ?: "网络错误"}") }

        if (!response.ok) throw ApiException(response.msg ?: "保存笔记失败")

        val mapping = HashMap<String, String>()
        response.files.forEach { fileDto ->
            val localId = fileDto.localFileId
            if (!localId.isNullOrBlank() && fileDto.fileId.isNotBlank()) {
                imageRepo.attachServerId(localId, fileDto.fileId)
                mapping[localId] = fileDto.fileId
            }
        }
        if (mapping.isNotEmpty()) {
            content = imageRepo.rewriteToServer(content, account, mapping)
        }

        val updated = note.copy(
            serverId = response.noteId.ifBlank { note.serverId },
            notebookServerId = response.notebookId.ifBlank { note.notebookServerId },
            usn = response.usn,
            isDirty = false,
            content = content,
            title = if (response.title.isNotBlank()) response.title else note.title,
            tags = if (response.tags.isNotEmpty()) response.tags.joinToString(",") else note.tags,
            urlTitle = response.urlTitle.ifBlank { note.urlTitle },
            createdTime = TimeUtils.parseServerTime(response.createdTime).takeIf { it > 0 } ?: note.createdTime,
            updatedTime = TimeUtils.parseServerTime(response.updatedTime).takeIf { it > 0 } ?: System.currentTimeMillis(),
        )
        noteDao.upsert(updated)
        PushResult.SUCCESS
    }

    suspend fun pushAllDirty(account: Account): IntArray = withContext(Dispatchers.IO) {
        val dirty = noteDao.getDirty(account.id)
        var pushed = 0
        var conflicts = 0
        var failed = 0
        dirty.forEach { note ->
            val result = runCatching { push(note.localId, account) }
                .getOrElse {
                    failed++
                    PushResult.SKIPPED
                }
            when (result) {
                PushResult.SUCCESS -> pushed++
                PushResult.CONFLICT -> conflicts++
                else -> Unit
            }
        }
        intArrayOf(pushed, conflicts, failed)
    }

    private fun NoteDto.toEntity(accountId: String, localId: Long = 0): NoteEntity {
        val created = TimeUtils.parseServerTime(createdTime)
        val updated = TimeUtils.parseServerTime(updatedTime)
        return NoteEntity(
            localId = localId,
            accountId = accountId,
            serverId = noteId,
            notebookServerId = notebookId,
            title = title,
            content = content,
            isMarkdown = isMarkdown,
            tags = tags.filter { it.isNotBlank() }.joinToString(","),
            isTrash = isTrash,
            isBlog = isBlog,
            urlTitle = urlTitle,
            usn = usn,
            createdTime = if (created > 0) created else System.currentTimeMillis(),
            updatedTime = if (updated > 0) updated else System.currentTimeMillis(),
            isDirty = false,
        )
    }

    suspend fun applyRemote(dto: NoteDto, accountId: String) = withContext(Dispatchers.IO) {
        val existing = noteDao.getByServerId(accountId, dto.noteId)
        noteDao.upsert(dto.toEntity(accountId, existing?.localId ?: 0))
    }

    /**
     * 拉取远端笔记时应用变更；若本地有未同步修改，则把本地内容另存为冲突副本。
     */
    suspend fun applyRemoteWithConflict(dto: NoteDto, accountId: String): Boolean = withContext(Dispatchers.IO) {
        val existing = noteDao.getByServerId(accountId, dto.noteId)
        if (existing != null && existing.isDirty) {
            val now = System.currentTimeMillis()
            noteDao.insert(
                existing.copy(
                    localId = 0,
                    serverId = "",
                    usn = 0,
                    title = existing.title.ifBlank { "无标题笔记" } + "（冲突副本）",
                    isDirty = true,
                    isTrash = false,
                    createdTime = now,
                    updatedTime = now,
                )
            )
        }
        noteDao.upsert(dto.toEntity(accountId, existing?.localId ?: 0))
        true
    }

    suspend fun findByServerId(accountId: String, serverId: String) = noteDao.getByServerId(accountId, serverId)

    private fun String.toPart(): RequestBody = toRequestBody("text/plain".toMediaType())
}

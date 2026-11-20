package com.leanote.next.data.repo

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.leanote.next.data.local.AttachmentDao
import com.leanote.next.data.local.NoteAttachmentEntity
import com.leanote.next.data.local.NoteEntity
import com.leanote.next.data.model.Account
import com.leanote.next.data.model.ApiException
import com.leanote.next.data.remote.ApiClient
import com.leanote.next.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentRepository @Inject constructor(
    private val attachmentDao: AttachmentDao,
    private val apiClient: ApiClient,
) {

    fun observe(noteLocalId: Long): Flow<List<NoteAttachmentEntity>> = attachmentDao.observe(noteLocalId)

    suspend fun refresh(account: Account, note: NoteEntity) = withContext(Dispatchers.IO) {
        if (note.serverId.isBlank()) return@withContext
        val remote = runCatching {
            apiClient.api(account.host).getAttachs(note.serverId, account.token)
        }.getOrElse { throw ApiException("获取附件列表失败：${it.message ?: "网络错误"}") }

        val existing = attachmentDao.listOfNote(note.localId)
        val matched = mutableSetOf<Long>()
        remote.filter { it.attachId.isNotBlank() }.forEach { dto ->
            val local = existing.firstOrNull { it.localId !in matched && it.serverId == dto.attachId }
                ?: existing.firstOrNull {
                    it.localId !in matched && it.serverId.isNullOrBlank() &&
                        it.fileName == dto.name && (dto.size <= 0L || it.size == dto.size)
                }
            val created = TimeUtils.parseServerTime(dto.createdTime).takeIf { it > 0L }
                ?: local?.createdTime ?: System.currentTimeMillis()
            val entity = NoteAttachmentEntity(
                localId = local?.localId ?: 0,
                noteLocalId = note.localId,
                accountId = account.id,
                serverId = dto.attachId,
                fileName = dto.name.ifBlank { local?.fileName ?: "未命名附件" },
                mimeType = dto.type.ifBlank { local?.mimeType ?: "application/octet-stream" },
                size = if (dto.size > 0L) dto.size else local?.size ?: 0L,
                localPath = local?.localPath,
                createdTime = created,
            )
            val localId = attachmentDao.upsert(entity)
            matched += if (local != null) local.localId else localId
        }

        val remoteIds = remote.map { it.attachId }.filter { it.isNotBlank() }.toSet()
        existing.filter { local ->
            val serverId = local.serverId
            local.localId !in matched && serverId != null && serverId !in remoteIds
        }.forEach { stale ->
            attachmentDao.delete(stale)
            stale.localPath?.let { File(it).delete() }
        }
    }

    suspend fun upload(
        context: Context,
        account: Account,
        note: NoteEntity,
        uri: Uri,
    ): NoteAttachmentEntity = withContext(Dispatchers.IO) {
        if (note.serverId.isBlank()) throw ApiException("请先同步笔记后再上传附件")

        val metadata = context.fileMetadata(uri)
        val directory = attachmentDirectory(context, note.localId)
        val target = File(directory, "${UUID.randomUUID()}-${safeFileName(metadata.name)}")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: throw ApiException("无法读取所选文件")
        } catch (error: Throwable) {
            target.delete()
            if (error is ApiException) throw error
            throw ApiException("保存附件失败：${error.message ?: "无法读取所选文件"}")
        }

        val response = runCatching {
            val body = target.asRequestBody(metadata.mimeType.toMediaType())
            val part = MultipartBody.Part.createFormData("file", metadata.name, body)
            apiClient.api(account.host).uploadAttach(part, note.serverId, account.token)
        }.getOrElse {
            target.delete()
            throw ApiException("上传附件失败：${it.message ?: "网络错误"}")
        }
        if (!response.ok) {
            target.delete()
            throw ApiException(response.msg ?: "上传附件失败")
        }

        val entity = NoteAttachmentEntity(
            noteLocalId = note.localId,
            accountId = account.id,
            serverId = response.attachId.ifBlank { response.id }.takeIf { it.isNotBlank() },
            fileName = metadata.name,
            mimeType = metadata.mimeType,
            size = if (metadata.size >= 0L) metadata.size else target.length(),
            localPath = target.absolutePath,
            createdTime = System.currentTimeMillis(),
        )
        val localId = attachmentDao.upsert(entity)
        entity.copy(localId = localId)
    }

    suspend fun open(context: Context, account: Account, attachment: NoteAttachmentEntity) {
        val file = ensureLocalFile(context, account, attachment)
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, attachment.mimeType.ifBlank { "application/octet-stream" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            throw ApiException("没有可打开此附件的应用")
        }
    }

    suspend fun delete(account: Account, attachment: NoteAttachmentEntity) = withContext(Dispatchers.IO) {
        val serverId = attachment.serverId
        if (!serverId.isNullOrBlank()) {
            val response = runCatching {
                apiClient.api(account.host).deleteAttach(serverId, account.token)
            }.getOrElse { throw ApiException("删除附件失败：${it.message ?: "网络错误"}") }
            if (!response.ok) throw ApiException(response.msg ?: "删除附件失败")
        }
        attachmentDao.delete(attachment)
        attachment.localPath?.let { File(it).delete() }
    }

    private suspend fun ensureLocalFile(
        context: Context,
        account: Account,
        attachment: NoteAttachmentEntity,
    ): File = withContext(Dispatchers.IO) {
        attachment.localPath?.let { path ->
            val cached = File(path)
            if (cached.isFile) return@withContext cached
        }
        val serverId = attachment.serverId
        if (serverId.isNullOrBlank()) throw ApiException("附件缺少服务器文件标识，无法下载")

        val directory = attachmentDirectory(context, attachment.noteLocalId)
        val target = attachment.localPath?.let(::File)
            ?: File(directory, "${attachment.localId}-${safeFileName(attachment.fileName)}")
        val temporary = File(directory, "${target.name}.download")
        temporary.delete()
        val connection = URL(apiClient.attachmentUrl(account.host, serverId, account.token))
            .openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            if (connection.responseCode !in 200..299) {
                throw ApiException("下载附件失败（HTTP ${connection.responseCode}）")
            }
            connection.inputStream.use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (error: Throwable) {
            temporary.delete()
            if (error is ApiException) throw error
            throw ApiException("下载附件失败：${error.message ?: "网络错误"}")
        } finally {
            connection.disconnect()
        }

        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }
        val updated = attachment.copy(
            localPath = target.absolutePath,
            size = if (attachment.size > 0L) attachment.size else target.length(),
        )
        attachmentDao.upsert(updated)
        target
    }

    private fun attachmentDirectory(context: Context, noteLocalId: Long): File =
        File(context.filesDir, "attachments/$noteLocalId").apply { mkdirs() }

    private fun Context.fileMetadata(uri: Uri): SelectedFile {
        var name: String? = null
        var size = -1L
        runCatching {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }
        }
        val displayName = name?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null }
            ?: "未命名附件"
        val extension = displayName.substringAfterLast('.', "").lowercase()
        val mimeType = contentResolver.getType(uri)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
        return SelectedFile(displayName, mimeType, size)
    }

    private fun safeFileName(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "attachment" }.take(120)

    private data class SelectedFile(
        val name: String,
        val mimeType: String,
        val size: Long,
    )
}

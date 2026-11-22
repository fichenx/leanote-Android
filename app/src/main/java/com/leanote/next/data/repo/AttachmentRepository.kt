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
import com.leanote.next.data.local.NoteDao
import com.leanote.next.data.local.NoteEntity
import com.leanote.next.data.model.Account
import com.leanote.next.data.model.ApiException
import com.leanote.next.data.remote.ApiClient
import com.leanote.next.data.remote.dto.NoteDto
import com.leanote.next.data.remote.dto.NoteFileDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentRepository @Inject constructor(
    private val attachmentDao: AttachmentDao,
    private val noteDao: NoteDao,
    private val apiClient: ApiClient,
) {

    fun observe(noteLocalId: Long): Flow<List<NoteAttachmentEntity>> = attachmentDao.observe(noteLocalId)

    suspend fun refresh(account: Account, note: NoteEntity) = withContext(Dispatchers.IO) {
        if (note.serverId.isBlank()) return@withContext
        val remote = remoteNote(account, note.serverId, "获取附件列表失败")
        val remoteAttachments = remote.attachments()

        val existing = attachmentDao.listOfNote(note.localId)
        val matched = mutableSetOf<Long>()
        remoteAttachments.forEach { dto ->
            val fileName = dto.title?.takeIf { it.isNotBlank() }
            val local = existing.firstOrNull { it.localId !in matched && it.serverId == dto.fileId }
                ?: existing.firstOrNull {
                    it.localId !in matched && it.serverId.isNullOrBlank() &&
                        fileName != null && it.fileName == fileName
                }
            val entity = NoteAttachmentEntity(
                localId = local?.localId ?: 0,
                noteLocalId = note.localId,
                accountId = account.id,
                serverId = dto.fileId,
                fileName = fileName ?: local?.fileName ?: "未命名附件",
                mimeType = remoteMimeType(dto, local?.mimeType.orEmpty()),
                size = local?.size ?: 0L,
                localPath = local?.localPath,
                createdTime = local?.createdTime ?: System.currentTimeMillis(),
            )
            val localId = attachmentDao.upsert(entity)
            matched += if (local != null) local.localId else localId
        }

        val remoteIds = remoteAttachments.map { it.fileId }.toSet()
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

        val localFileId = UUID.randomUUID().toString()
        val response = try {
            val remote = remoteNote(account, note.serverId, "上传附件失败")
            updateAttachments(
                account = account,
                note = note,
                remote = remote,
                retained = remote.attachments(),
                upload = PendingUpload(localFileId, metadata.name, metadata.mimeType, target),
                errorLabel = "上传附件失败",
            )
        } catch (error: Throwable) {
            target.delete()
            if (error is ApiException) throw error
            throw ApiException("上传附件失败：${error.message ?: "网络错误"}")
        }
        val serverId = response.files.firstOrNull {
            it.localFileId == localFileId && it.fileId.isNotBlank()
        }?.fileId
        if (serverId == null) {
            target.delete()
            throw ApiException("上传附件失败：服务器未返回文件标识")
        }

        val entity = NoteAttachmentEntity(
            noteLocalId = note.localId,
            accountId = account.id,
            serverId = serverId,
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
            val note = noteDao.getById(attachment.noteLocalId)
                ?: throw ApiException("删除附件失败：找不到所属笔记")
            if (note.serverId.isBlank()) throw ApiException("请先同步笔记后再删除附件")
            val remote = remoteNote(account, note.serverId, "删除附件失败")
            updateAttachments(
                account = account,
                note = note,
                remote = remote,
                retained = remote.attachments().filter { it.fileId != serverId },
                upload = null,
                errorLabel = "删除附件失败",
            )
        }
        attachmentDao.delete(attachment)
        attachment.localPath?.let { File(it).delete() }
    }

    private suspend fun remoteNote(account: Account, noteId: String, errorLabel: String): NoteDto {
        val remote = runCatching {
            apiClient.api(account.host).noteMeta(noteId, account.token)
        }.getOrElse { throw ApiException("$errorLabel：${it.message ?: "网络错误"}") }
        if (!remote.ok) {
            throw ApiException(remote.msg?.takeIf { it.isNotBlank() } ?: errorLabel)
        }
        return remote
    }

    private suspend fun updateAttachments(
        account: Account,
        note: NoteEntity,
        remote: NoteDto,
        retained: List<NoteFileDto>,
        upload: PendingUpload?,
        errorLabel: String,
    ): NoteDto {
        val fields = LinkedHashMap<String, RequestBody>()
        val parts = mutableListOf<MultipartBody.Part>()
        fields["NoteId"] = note.serverId.toPart()
        fields["Usn"] = remote.usn.toString().toPart()
        // UpdateNote 只改附件时会返回失败，回传服务器标题以完成有效更新并获取新 USN。
        fields["Title"] = remote.title.toPart()

        retained.forEachIndexed { index, file ->
            fields["Files[$index][LocalFileId]"] = "".toPart()
            fields["Files[$index][FileId]"] = file.fileId.toPart()
            fields["Files[$index][IsAttach]"] = "1".toPart()
            fields["Files[$index][HasBody]"] = "0".toPart()
            fields["Files[$index][Title]"] = file.title.orEmpty().toPart()
            fields["Files[$index][Type]"] = file.type.orEmpty().toPart()
        }

        if (upload != null) {
            val index = retained.size
            fields["Files[$index][LocalFileId]"] = upload.localFileId.toPart()
            fields["Files[$index][FileId]"] = "".toPart()
            fields["Files[$index][IsAttach]"] = "1".toPart()
            fields["Files[$index][HasBody]"] = "1".toPart()
            fields["Files[$index][Title]"] = upload.fileName.toPart()
            fields["Files[$index][Type]"] = upload.fileName.substringAfterLast('.', "").toPart()
            val body = upload.file.asRequestBody(upload.mimeType.toMediaType())
            parts += MultipartBody.Part.createFormData(
                "FileDatas[${upload.localFileId}]",
                upload.fileName,
                body,
            )
        }

        val response = runCatching {
            apiClient.api(account.host).updateNote(fields, parts, account.token)
        }.getOrElse { throw ApiException("$errorLabel：${it.message ?: "网络错误"}") }
        if (!response.ok) {
            throw ApiException(response.msg?.takeIf { it.isNotBlank() } ?: errorLabel)
        }
        if (response.usn > 0) noteDao.updateUsn(note.localId, response.usn)
        return response
    }

    private fun NoteDto.attachments(): List<NoteFileDto> =
        files.filter { it.isAttach && it.fileId.isNotBlank() }

    private fun remoteMimeType(file: NoteFileDto, fallback: String): String {
        val serverType = file.type?.trim()?.trimStart('.')?.lowercase().orEmpty()
        val fromType = if (serverType.contains('/')) {
            serverType
        } else {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(serverType)
        }
        val extension = file.title?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return fromType
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: fallback.takeIf { it.isNotBlank() }
            ?: "application/octet-stream"
    }

    private fun String.toPart(): RequestBody = toRequestBody("text/plain".toMediaType())

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

    private data class PendingUpload(
        val localFileId: String,
        val fileName: String,
        val mimeType: String,
        val file: File,
    )

    private data class SelectedFile(
        val name: String,
        val mimeType: String,
        val size: Long,
    )
}

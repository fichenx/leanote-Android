package com.leanote.next.data.repo

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.leanote.next.data.local.ImageDao
import com.leanote.next.data.local.NoteImageEntity
import com.leanote.next.data.model.Account
import com.leanote.next.data.remote.ApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 管理笔记内的图片：从相册导入、本地文件与服务器 fileId 的映射、渲染前的内容改写。
 */
@Singleton
class ImageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageDao: ImageDao,
    private val apiClient: ApiClient,
) {

    private val imageDir: File by lazy {
        File(context.filesDir, "note_images").apply { mkdirs() }
    }

    suspend fun importImage(uri: Uri, noteLocalId: Long): NoteImageEntity? = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        if (!mime.startsWith("image/")) return@withContext null
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
        val localId = UUID.randomUUID().toString().replace("-", "")
        val target = File(imageDir, "$localId.$ext")
        val ok = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: false
        }.isSuccess
        if (!ok || !target.isFile) {
            target.delete()
            return@withContext null
        }
        val entity = NoteImageEntity(
            localId = localId,
            noteLocalId = noteLocalId,
            localPath = target.absolutePath,
            mimeType = mime,
        )
        imageDao.insert(entity)
        entity
    }

    suspend fun register(noteLocalId: Long, file: File, mime: String): NoteImageEntity {
        val localId = UUID.randomUUID().toString().replace("-", "")
        val target = File(imageDir, "$localId.${MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"}")
        file.copyTo(target, overwrite = true)
        val entity = NoteImageEntity(localId, noteLocalId, null, target.absolutePath, mime)
        imageDao.insert(entity)
        return entity
    }

    suspend fun getByLocalId(localId: String): NoteImageEntity? = imageDao.getByLocalId(localId)

    suspend fun getByServerId(serverId: String): NoteImageEntity? = imageDao.getByServerId(serverId)

    suspend fun listOfNote(noteLocalId: Long): List<NoteImageEntity> = imageDao.listOfNote(noteLocalId)

    suspend fun listByLocalIds(ids: List<String>): List<NoteImageEntity> =
        if (ids.isEmpty()) emptyList() else imageDao.listByLocalIds(ids)

    suspend fun attachServerId(localId: String, serverId: String) =
        imageDao.updateServerId(localId, serverId)

    suspend fun deleteOfNote(noteLocalId: Long) = imageDao.deleteOfNote(noteLocalId)

    /** 把内容中的图片引用改写成本地可加载的地址（本地文件优先），并为远程图片补上 token。 */
    suspend fun prepareForRender(content: String, account: Account): String {
        val localIds = ImageRefs.localIds(content)
        val localMap = if (localIds.isEmpty()) {
            emptyMap()
        } else {
            imageDao.listByLocalIds(localIds)
                .mapNotNull { image ->
                    val file = File(image.localPath)
                    if (file.isFile) image.localId to Uri.fromFile(file).toString() else null
                }
                .toMap()
        }
        var result = ImageRefs.replaceLocalIds(content, localMap)

        val remoteIds = ImageRefs.remotePattern.findAll(result).map { it.groupValues[2] }.distinct().toList()
        val remoteMap = HashMap<String, String>()
        remoteIds.forEach { fileId ->
            val cached = imageDao.getByServerId(fileId)?.let { File(it.localPath) }
            remoteMap[fileId] = if (cached != null && cached.isFile) {
                Uri.fromFile(cached).toString()
            } else {
                apiClient.imageUrl(account.host, fileId, account.token)
            }
        }
        result = ImageRefs.remotePattern.replace(result) { match ->
            remoteMap[match.groupValues[2]] ?: match.value
        }
        return result
    }

    /** 上传完成后把本地引用替换为服务器地址。 */
    fun rewriteToServer(content: String, account: Account, mapping: Map<String, String>): String =
        ImageRefs.replaceLocalIds(content, mapping.mapValues { (_, fileId) ->
            apiClient.imageUrl(account.host, fileId)
        })

    /** WebView 渲染前的改写：本地图片走拦截地址，远程图片优先使用缓存文件。 */
    suspend fun prepareForWeb(content: String, account: Account): String {
        val localIds = ImageRefs.localIds(content)
        var result = ImageRefs.replaceLocalIds(content, localIds.associateWith { localImageUrl(it) })

        val remoteIds = ImageRefs.remotePattern.findAll(result).map { it.groupValues[2] }.distinct().toList()
        val remoteMap = HashMap<String, String>()
        remoteIds.forEach { fileId ->
            val cached = imageDao.getByServerId(fileId)
            remoteMap[fileId] = if (cached != null) {
                serverImageUrl(fileId)
            } else {
                apiClient.imageUrl(account.host, fileId, account.token)
            }
        }
        result = ImageRefs.remotePattern.replace(result) { match ->
            remoteMap[match.groupValues[2]] ?: match.value
        }
        return result
    }

    fun localImageUrl(localId: String): String = "$WEB_HOST/getImage?localId=$localId"

    fun serverImageUrl(serverId: String): String = "$WEB_HOST/getImage?serverId=$serverId"

    /** 富文本编辑器保存前把拦截地址还原成笔记内容中的标准引用。 */
    fun restoreFromWeb(content: String, account: Account): String {
        var result = ImageRefs.webLocalPattern.replace(content) { match ->
            "file:/getImage?id=${match.groupValues[1]}"
        }
        result = ImageRefs.webServerPattern.replace(result) { match ->
            apiClient.imageUrl(account.host, match.groupValues[1])
        }
        result = ImageRefs.remotePattern.replace(result) { match -> match.groupValues[1] }
        return result
    }

    /** 内容中已经被上传过的本地图片（用于 updateNote 时回传 FileId 元数据）。 */
    suspend fun uploadedRefs(content: String): List<NoteImageEntity> {
        val ids = ImageRefs.localIds(content)
        return listByLocalIds(ids).filter { !it.serverId.isNullOrBlank() }
    }

    /** WebView 图片请求拦截：把本地/缓存图片映射成可读数据。 */
    suspend fun findLocalImage(url: String): Pair<File, String>? {
        if (url.startsWith(WEB_HOST)) {
            val parsed = Uri.parse(url)
            val localId = parsed.getQueryParameter("localId")
            if (!localId.isNullOrBlank()) {
                val entity = imageDao.getByLocalId(localId) ?: return null
                val file = File(entity.localPath)
                return if (file.isFile) file to entity.mimeType else null
            }
            val serverId = parsed.getQueryParameter("serverId")
            if (!serverId.isNullOrBlank()) {
                val entity = imageDao.getByServerId(serverId) ?: return null
                val file = File(entity.localPath)
                return if (file.isFile) file to entity.mimeType else null
            }
            return null
        }
        val local = ImageRefs.localPattern.find(url)
        if (local != null) {
            val entity = imageDao.getByLocalId(local.groupValues[1]) ?: return null
            val file = File(entity.localPath)
            return if (file.isFile) file to entity.mimeType else null
        }
        val remote = ImageRefs.remotePattern.find(url)
        if (remote != null) {
            val entity = imageDao.getByServerId(remote.groupValues[2]) ?: return null
            val file = File(entity.localPath)
            return if (file.isFile) file to entity.mimeType else null
        }
        return null
    }

    fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()
}

private const val WEB_HOST = "https://leanote.local"

private object ImageRefs {
    val localPattern = Regex("""file:/+getImage\?id=([A-Za-z0-9_\-]+)""")
    val webLocalPattern = Regex("""https://leanote\.local/getImage\?localId=([A-Za-z0-9_\-]+)""")
    val webServerPattern = Regex("""https://leanote\.local/getImage\?serverId=([A-Za-z0-9_\-]+)""")
    val remotePattern = Regex(
        """(https?://[^"'\s()<>]+?/api/file/getImage\?fileId=([A-Za-z0-9_\-]+))(?:&token=[^"'\s()<>]*)?"""
    )

    fun localIds(content: String): List<String> =
        localPattern.findAll(content).map { it.groupValues[1] }.distinct().toList()

    fun replaceLocalIds(content: String, mapping: Map<String, String>): String =
        localPattern.replace(content) { match -> mapping[match.groupValues[1]] ?: match.value }
}

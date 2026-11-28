package com.leanote.next.data.transfer

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Xml
import android.webkit.MimeTypeMap
import com.leanote.next.data.local.ImageDao
import com.leanote.next.data.local.NoteDao
import com.leanote.next.data.local.NoteEntity
import com.leanote.next.data.local.NoteImageEntity
import com.leanote.next.data.local.NotebookDao
import com.leanote.next.data.local.NotebookEntity
import com.leanote.next.data.model.AppSettings
import com.leanote.next.data.model.SortMode
import com.leanote.next.data.model.ThemeMode
import com.leanote.next.data.model.ThemePalette
import com.leanote.next.data.prefs.AccountStore
import com.leanote.next.data.prefs.SettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.StringReader
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class ExportFormat(val label: String, val extension: String) {
    MARKDOWN("Markdown", "md"),
    HTML("HTML", "html"),
    ENEX("Evernote ENEX", "enex"),
    LEANOTE("蚂蚁笔记", "leanote"),
}

@Singleton
class NoteTransfer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteDao: NoteDao,
    private val notebookDao: NotebookDao,
    private val imageDao: ImageDao,
    private val accountStore: AccountStore,
    private val settingsStore: SettingsStore,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val imageDir: File by lazy {
        File(context.filesDir, "note_images").apply { mkdirs() }
    }

    suspend fun exportNotes(
        notes: List<NoteEntity>,
        targetDir: Uri,
        format: ExportFormat,
    ): Int = withContext(Dispatchers.IO) {
        if (notes.isEmpty()) return@withContext 0
        var exported = 0
        try {
            val usedNames = existingDisplayNames(targetDir)
            when (format) {
                ExportFormat.ENEX -> {
                    val name = uniqueFileName(
                        base = "leanote_notes_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()),
                        extension = ExportFormat.ENEX.extension,
                        usedNames = usedNames,
                    )
                    if (writeTextDocument(targetDir, "application/xml", name, buildEnex(notes))) {
                        exported = notes.size
                    }
                }

                ExportFormat.LEANOTE -> {
                    val name = uniqueFileName(
                        base = "leanote_notes_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()),
                        extension = ExportFormat.LEANOTE.extension,
                        usedNames = usedNames,
                    )
                    if (writeTextDocument(targetDir, "application/json", name, buildLeanoteExport(notes))) {
                        exported = notes.size
                    }
                }

                ExportFormat.MARKDOWN, ExportFormat.HTML -> notes.forEach { note ->
                    try {
                        // Markdown 导出保留源文；富文本始终使用完整 HTML，避免把 HTML 塞进 .md。
                        val asHtml = format == ExportFormat.HTML || !note.isMarkdown
                        val extension = if (asHtml) "html" else "md"
                        val content = if (asHtml) buildHtml(note) else note.content
                        val name = uniqueFileName(safeFileBase(note.title), extension, usedNames)
                        val mime = if (asHtml) "text/html" else "text/markdown"
                        if (writeTextDocument(targetDir, mime, name, content)) exported++
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // 单篇失败不影响其他笔记，调用方可根据返回数量判断是否有遗漏。
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // SAF 目录不可用或整体写入失败时返回已经成功的数量。
        }
        exported
    }

    suspend fun importFiles(uris: List<Uri>, notebookServerId: String): Pair<Int, String> =
        withContext(Dispatchers.IO) {
            if (uris.isEmpty()) return@withContext 0 to "未选择文件"
            val account = accountStore.current() ?: return@withContext 0 to "请先登录账户"
            val targetNotebook = notebookServerId.ifBlank {
                notebookDao.getAll(account.id)
                    .firstOrNull { it.parentServerId.isBlank() }
                    ?.serverId
                    .orEmpty()
            }
            if (targetNotebook.isBlank()) return@withContext 0 to "未找到可用笔记本，请先创建笔记本"

            var imported = 0
            val errors = mutableListOf<String>()
            uris.forEach { uri ->
                val displayName = displayName(uri) ?: "未命名文件"
                val extension = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
                if (extension !in SUPPORTED_IMPORT_EXTENSIONS) {
                    errors.add("$displayName：不支持的文件格式")
                    return@forEach
                }
                try {
                    val text = readText(uri)
                    when (extension) {
                        "md", "markdown", "txt" -> {
                            insertImportedNote(
                                accountId = account.id,
                                notebookId = targetNotebook,
                                title = fileTitle(displayName),
                                content = text,
                                markdown = true,
                            )
                            imported++
                        }

                        "html", "htm" -> {
                            insertImportedNote(
                                accountId = account.id,
                                notebookId = targetNotebook,
                                title = fileTitle(displayName),
                                content = extractHtmlBody(text),
                                markdown = false,
                            )
                            imported++
                        }

                        "enex" -> {
                            val notes = parseEnex(text)
                            if (notes.isEmpty()) errors.add("$displayName：未找到笔记")
                            notes.forEachIndexed { index, note ->
                                try {
                                    insertImportedNote(
                                        accountId = account.id,
                                        notebookId = targetNotebook,
                                        title = note.title.ifBlank { "无标题笔记" },
                                        content = extractEnoteBody(note.content),
                                        markdown = false,
                                        tags = note.tags.joinToString(","),
                                    )
                                    imported++
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    errors.add("$displayName 第 ${index + 1} 篇：${e.readableMessage("写入失败")}")
                                }
                            }
                        }

                        "leanote" -> {
                            val root = Json.parseToJsonElement(text).jsonObject
                            val noteArray = root["notes"]?.jsonArray ?: JsonArray(emptyList())
                            if (noteArray.isEmpty()) errors.add("$displayName：未找到笔记")
                            noteArray.forEachIndexed { index, element ->
                                try {
                                    imported += importLeanoteNote(account.id, targetNotebook, element.jsonObject)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    errors.add("$displayName 第 ${index + 1} 篇：${e.readableMessage("写入失败")}")
                                }
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    errors.add("$displayName：${e.readableMessage("读取或解析失败")}")
                }
            }
            imported to summarizeErrors(errors)
        }

    suspend fun backup(target: Uri): Int = withContext(Dispatchers.IO) {
        val account = accountStore.current() ?: return@withContext 0
        try {
            val notes = noteDao.observeAll(account.id).first()
            val notebooks = notebookDao.getAll(account.id)
            val settings = settingsStore.settings.first()
            val images = collectImages(notes)
            val meta = BackupMetaDto(
                accountId = account.id,
                createdAt = System.currentTimeMillis(),
                images = images.map { it.metadata },
            )
            val output = context.contentResolver.openOutputStream(target) ?: return@withContext 0
            output.use { rawOutput ->
                ZipOutputStream(BufferedOutputStream(rawOutput)).use { zip ->
                    zip.writeTextEntry("meta.json", json.encodeToString(meta))
                    zip.writeTextEntry("notes.json", json.encodeToString(notes.map { it.toBackupDto() }))
                    zip.writeTextEntry(
                        "notebooks.json",
                        json.encodeToString(notebooks.map { it.toBackupDto() }),
                    )
                    zip.writeTextEntry("settings.json", json.encodeToString(settings.toBackupDto()))
                    images.forEach { image ->
                        zip.putNextEntry(ZipEntry(image.metadata.entryName))
                        image.source.inputStream().use { input -> input.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
            notes.size
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            0
        }
    }

    suspend fun restore(source: Uri): Pair<Int, String> = withContext(Dispatchers.IO) {
        val account = accountStore.current() ?: return@withContext 0 to "请先登录账户"
        var noteDtos: List<NoteBackupDto>? = null
        var notebookDtos: List<NotebookBackupDto> = emptyList()
        var settingsDto: SettingsBackupDto? = null
        var meta: BackupMetaDto? = null
        val imageFiles = linkedMapOf<String, File>()
        val errors = mutableListOf<String>()

        try {
            val input = context.contentResolver.openInputStream(source)
                ?: return@withContext 0 to "无法打开备份文件"
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory) {
                        when (entry.name) {
                            "meta.json" -> meta = json.decodeFromString(zip.readUtf8())
                            "notes.json" -> noteDtos = json.decodeFromString(zip.readUtf8())
                            "notebooks.json" -> notebookDtos = json.decodeFromString(zip.readUtf8())
                            "settings.json" -> settingsDto = json.decodeFromString(zip.readUtf8())
                            else -> if (entry.name.startsWith("images/")) {
                                try {
                                    imageFiles[entry.name] = copyImageEntry(zip, entry.name)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    errors.add("图片 ${entry.name.substringAfterLast('/')}：${e.readableMessage("恢复失败")}")
                                }
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
        } catch (e: CancellationException) {
            imageFiles.values.forEach { it.delete() }
            throw e
        } catch (e: Exception) {
            imageFiles.values.forEach { it.delete() }
            return@withContext 0 to e.readableMessage("备份文件读取失败")
        }

        val notesToRestore = noteDtos
        if (notesToRestore == null) {
            imageFiles.values.forEach { it.delete() }
            return@withContext 0 to "备份文件缺少 notes.json"
        }

        settingsDto?.let { dto ->
            try {
                restoreSettings(dto)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errors.add("应用设置：${e.readableMessage("恢复失败")}")
            }
        }

        notebookDtos.forEach { dto ->
            try {
                val existing = if (dto.serverId.isNotBlank()) {
                    notebookDao.getByServerId(account.id, dto.serverId)
                } else {
                    notebookDao.getById(account.id, dto.localId)
                        ?.takeIf { it.serverId.isBlank() }
                }
                notebookDao.upsert(dto.toEntity(account.id, existing?.localId ?: 0))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errors.add("笔记本 ${dto.title.ifBlank { "无标题" }}：${e.readableMessage("恢复失败")}")
            }
        }

        var restored = 0
        val noteIdMap = mutableMapOf<Long, Long>()
        notesToRestore.forEach { dto ->
            try {
                val existing = if (dto.serverId.isNotBlank()) {
                    noteDao.getByServerId(account.id, dto.serverId)
                } else {
                    noteDao.getById(dto.localId)
                        ?.takeIf { it.accountId == account.id && it.serverId.isBlank() }
                }
                val localId = if (existing == null) {
                    noteDao.insert(dto.toEntity(account.id))
                } else {
                    noteDao.upsert(dto.toEntity(account.id, existing.localId))
                    existing.localId
                }
                if (dto.localId > 0) noteIdMap[dto.localId] = localId
                restored++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errors.add("笔记 ${dto.title.ifBlank { "无标题" }}：${e.readableMessage("恢复失败")}")
            }
        }

        val retainedImages = mutableSetOf<File>()
        meta?.images.orEmpty().forEach { dto ->
            val file = imageFiles[dto.entryName]
            val noteLocalId = noteIdMap[dto.noteLocalId]
            if (file == null || noteLocalId == null) return@forEach
            try {
                imageDao.insert(
                    NoteImageEntity(
                        localId = dto.localId.ifBlank { UUID.randomUUID().toString().replace("-", "") },
                        noteLocalId = noteLocalId,
                        serverId = dto.serverId,
                        localPath = file.absolutePath,
                        mimeType = dto.mimeType.ifBlank { "image/jpeg" },
                    ),
                )
                retainedImages.add(file)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errors.add("图片映射：${e.readableMessage("恢复失败")}")
            }
        }
        imageFiles.values.filterNot { it in retainedImages }.forEach { it.delete() }
        restored to summarizeErrors(errors)
    }

    private suspend fun insertImportedNote(
        accountId: String,
        notebookId: String,
        title: String,
        content: String,
        markdown: Boolean,
        tags: String = "",
    ) {
        val now = System.currentTimeMillis()
        noteDao.insert(
            NoteEntity(
                accountId = accountId,
                notebookServerId = notebookId,
                title = title.ifBlank { "无标题笔记" },
                content = content,
                isMarkdown = markdown,
                tags = tags,
                createdTime = now,
                updatedTime = now,
                isDirty = true,
            ),
        )
    }

    /** 蚂蚁笔记专用格式(.leanote): 与服务端/桌面版一致, 含全部正文与图片 base64 */
    private suspend fun buildLeanoteExport(notes: List<NoteEntity>): String {
        val noteObjects = mutableListOf<JsonObject>()
        notes.forEach { note ->
            var content = note.content
            val fileObjects = mutableListOf<JsonObject>()
            imageDao.listOfNote(note.localId).forEach { image ->
                val source = File(image.localPath)
                if (!source.isFile) return@forEach
                val fileId = UUID.randomUUID().toString().replace("-", "").take(24)
                content = content.replace(
                    "file:/getImage?id=${image.localId}",
                    "leanote://file/getImage?fileId=$fileId",
                )
                val serverId = image.serverId
                if (!serverId.isNullOrBlank()) {
                    val remote = Regex(
                        """https?://[^"'\s()<>]+/api/file/getImage\?fileId=$serverId(?:&token=[^"'\s()<>]*)?""",
                    )
                    content = remote.replace(content, "leanote://file/getImage?fileId=$fileId")
                }
                val extension = source.extension.ifBlank { "jpg" }
                fileObjects.add(
                    buildJsonObject {
                        put("fileId", fileId)
                        put("base64", Base64.encodeToString(source.readBytes(), Base64.NO_WRAP))
                        put("md5", "")
                        put("type", extension)
                        put("title", source.name)
                        put("isAttach", false)
                        put("createdTime", formatLeanoteTime(note.createdTime))
                    },
                )
            }
            noteObjects.add(
                buildJsonObject {
                    put("title", note.title)
                    put("content", content)
                    put("tags", JsonArray(note.tagList.map { JsonPrimitive(it) }))
                    put("isMarkdown", note.isMarkdown)
                    put("createdTime", formatLeanoteTime(note.createdTime))
                    put("updatedTime", formatLeanoteTime(note.updatedTime))
                    put("files", JsonArray(fileObjects))
                },
            )
        }
        return buildJsonObject {
            put("exportDate", formatLeanoteTime(System.currentTimeMillis()))
            put("app", "leanote.android")
            put("apiVersion", "0.1")
            put("notes", JsonArray(noteObjects))
        }.toString()
    }

    /** 导入 .leanote 单篇笔记, 图片落盘并改写为本地引用 */
    private suspend fun importLeanoteNote(accountId: String, notebookId: String, obj: JsonObject): Int {
        data class PendingImage(val fileId: String, val localId: String, val path: String, val mime: String)

        var content = obj["content"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val title = obj["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val markdown = obj["isMarkdown"]?.jsonPrimitive?.booleanOrNull ?: false
        val tags = obj["tags"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
        val createdTime = parseLeanoteTime(obj["createdTime"]?.jsonPrimitive?.contentOrNull)
        val updatedTime = parseLeanoteTime(obj["updatedTime"]?.jsonPrimitive?.contentOrNull)

        val pending = mutableListOf<PendingImage>()
        obj["files"]?.jsonArray?.forEach { element ->
            val file = element.jsonObject
            val fileId = file["fileId"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            val base64 = file["base64"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            if (file["isAttach"]?.jsonPrimitive?.booleanOrNull == true) return@forEach
            val bytes = runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull() ?: return@forEach
            val extension = file["type"]?.jsonPrimitive?.contentOrNull.orEmpty()
                .trimStart('.').ifBlank { "jpg" }
            val localId = UUID.randomUUID().toString().replace("-", "")
            val target = File(imageDir, "$localId.$extension")
            try {
                target.outputStream().use { it.write(bytes) }
            } catch (e: Exception) {
                return@forEach
            }
            val mime = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(extension.lowercase(Locale.ROOT)) ?: "image/jpeg"
            pending.add(PendingImage(fileId, localId, target.absolutePath, mime))
        }

        pending.forEach { image ->
            content = content.replace(
                "leanote://file/getImage?fileId=${image.fileId}",
                "file:/getImage?id=${image.localId}",
            )
        }

        val now = System.currentTimeMillis()
        val noteLocalId = noteDao.insert(
            NoteEntity(
                accountId = accountId,
                notebookServerId = notebookId,
                title = title.ifBlank { "无标题笔记" },
                content = content,
                isMarkdown = markdown,
                tags = tags.joinToString(","),
                createdTime = if (createdTime > 0) createdTime else now,
                updatedTime = if (updatedTime > 0) updatedTime else now,
                isDirty = true,
            ),
        )
        pending.forEach { image ->
            imageDao.insert(
                NoteImageEntity(
                    localId = image.localId,
                    noteLocalId = noteLocalId,
                    serverId = null,
                    localPath = image.path,
                    mimeType = image.mime,
                ),
            )
        }
        return 1
    }

    private fun formatLeanoteTime(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(millis))

    private fun parseLeanoteTime(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        runCatching { return Instant.parse(raw).toEpochMilli() }
        runCatching {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(raw)?.time ?: 0L
        }
        return 0L
    }

    private fun existingDisplayNames(treeUri: Uri): MutableSet<String> {        val result = mutableSetOf<String>()
        runCatching {
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri),
            )
            context.contentResolver.query(
                children,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)
                    if (!name.isNullOrBlank()) result.add(name.lowercase(Locale.ROOT))
                }
            }
        }
        return result
    }

    private fun writeTextDocument(treeUri: Uri, mime: String, name: String, content: String): Boolean {
        val parent = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val document = DocumentsContract.createDocument(context.contentResolver, parent, mime, name)
            ?: return false
        val output = context.contentResolver.openOutputStream(document, "w") ?: return false
        output.bufferedWriter(Charsets.UTF_8).use { it.write(content) }
        return true
    }

    private fun readText(uri: Uri): String {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("无法读取文件")
        return input.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun displayName(uri: Uri): String? {
        val queried = runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()
        return queried ?: uri.lastPathSegment?.substringAfterLast('/')
    }

    private suspend fun collectImages(notes: List<NoteEntity>): List<ImageToBackup> {
        val byLocalId = linkedMapOf<String, NoteImageEntity>()
        notes.forEach { note ->
            imageDao.listOfNote(note.localId).forEach { image ->
                if (File(image.localPath).isFile) byLocalId.putIfAbsent(image.localId, image)
            }
        }
        val usedEntryNames = mutableSetOf<String>()
        return byLocalId.values.map { image ->
            val source = File(image.localPath)
            val id = image.localId.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "image" }
            val extension = source.extension.replace(Regex("[^A-Za-z0-9]"), "").ifBlank { "jpg" }
            var entryName = "images/$id.$extension"
            var suffix = 2
            while (!usedEntryNames.add(entryName)) {
                entryName = "images/${id}_$suffix.$extension"
                suffix++
            }
            ImageToBackup(
                metadata = ImageBackupDto(
                    localId = image.localId,
                    noteLocalId = image.noteLocalId,
                    serverId = image.serverId,
                    entryName = entryName,
                    mimeType = image.mimeType,
                ),
                source = source,
            )
        }
    }

    private fun copyImageEntry(input: InputStream, entryName: String): File {
        imageDir.mkdirs()
        val rawName = entryName.substringAfterLast('/')
        val safeName = rawName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('.', ' ')
            .ifBlank { "image" }
        val target = File(imageDir, "${UUID.randomUUID().toString().replace("-", "")}_$safeName")
        try {
            target.outputStream().use { output -> input.copyTo(output) }
            return target
        } catch (e: Exception) {
            target.delete()
            throw e
        }
    }

    private fun ZipOutputStream.writeTextEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun InputStream.readUtf8(): String = String(readBytes(), Charsets.UTF_8)

    private suspend fun restoreSettings(dto: SettingsBackupDto) {
        val current = settingsStore.settings.first()
        settingsStore.setThemeMode(enumValueOr(dto.themeMode, ThemeMode.SYSTEM))
        settingsStore.setPalette(enumValueOr(dto.palette, ThemePalette.FOREST))
        settingsStore.setDynamicColor(dto.dynamicColor)
        dto.surfaceEffect?.let { value ->
            settingsStore.setSurfaceEffect(enumValueOr(value, current.surfaceEffect))
        }
        dto.surfaceTranslucency?.let { settingsStore.setSurfaceTranslucency(it.coerceIn(0, 60)) }
        settingsStore.clearCustomColors()
        dto.customColors.forEach { (role, hex) -> settingsStore.setCustomColor(role, hex) }
        dto.density?.let { value ->
            settingsStore.setDensity(enumValueOr(value, current.density))
        }
        dto.noteCardBorder?.let { settingsStore.setNoteCardBorder(it) }
        dto.noteCardCorner?.let { settingsStore.setNoteCardCorner(it.coerceIn(0, 28)) }
        dto.uiScale?.let { settingsStore.setUiScale(it.coerceIn(85, 130)) }
        settingsStore.setAutoSync(dto.autoSync)
        settingsStore.setSortMode(enumValueOr(dto.sortMode, SortMode.UPDATED_DESC))
        settingsStore.setDefaultMarkdown(dto.defaultMarkdown)
    }

    private fun NoteEntity.toBackupDto() = NoteBackupDto(
        localId = localId,
        serverId = serverId,
        notebookServerId = notebookServerId,
        title = title,
        content = content,
        isMarkdown = isMarkdown,
        tags = tags,
        isTrash = isTrash,
        isBlog = isBlog,
        urlTitle = urlTitle,
        usn = usn,
        createdTime = createdTime,
        updatedTime = updatedTime,
        isDirty = isDirty,
    )

    private fun NotebookEntity.toBackupDto() = NotebookBackupDto(
        localId = localId,
        serverId = serverId,
        parentServerId = parentServerId,
        title = title,
        seq = seq,
        usn = usn,
    )

    private fun AppSettings.toBackupDto() = SettingsBackupDto(
        themeMode = themeMode.name,
        palette = palette.name,
        dynamicColor = dynamicColor,
        surfaceEffect = surfaceEffect.name,
        surfaceTranslucency = surfaceTranslucency,
        customColors = customColors,
        density = density.name,
        noteCardBorder = noteCardBorder,
        noteCardCorner = noteCardCorner,
        uiScale = uiScale,
        autoSync = autoSync,
        sortMode = sortMode.name,
        defaultMarkdown = defaultMarkdown,
    )

    private fun NoteBackupDto.toEntity(accountId: String, localId: Long = 0) = NoteEntity(
        localId = localId,
        accountId = accountId,
        serverId = serverId,
        notebookServerId = notebookServerId,
        title = title,
        content = content,
        isMarkdown = isMarkdown,
        tags = tags,
        isTrash = isTrash,
        isBlog = isBlog,
        urlTitle = urlTitle,
        usn = usn,
        createdTime = createdTime,
        updatedTime = updatedTime,
        isDirty = isDirty,
    )

    private fun NotebookBackupDto.toEntity(accountId: String, localId: Long = 0) = NotebookEntity(
        localId = localId,
        accountId = accountId,
        serverId = serverId,
        parentServerId = parentServerId,
        title = title,
        seq = seq,
        usn = usn,
    )
}

private val SUPPORTED_IMPORT_EXTENSIONS = setOf("md", "markdown", "txt", "html", "htm", "enex", "leanote")
private val ENEX_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US)
    .withZone(ZoneOffset.UTC)
private val INVALID_XML_CONTROLS = Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]")

private data class ImageToBackup(val metadata: ImageBackupDto, val source: File)

private data class ImportedNote(
    var title: String = "",
    var content: String = "",
    val tags: MutableList<String> = mutableListOf(),
)

@Serializable
private data class BackupMetaDto(
    val version: Int = 1,
    val accountId: String = "",
    val createdAt: Long = 0,
    val images: List<ImageBackupDto> = emptyList(),
)

@Serializable
private data class NoteBackupDto(
    val localId: Long = 0,
    val serverId: String = "",
    val notebookServerId: String = "",
    val title: String = "",
    val content: String = "",
    val isMarkdown: Boolean = true,
    val tags: String = "",
    val isTrash: Boolean = false,
    val isBlog: Boolean = false,
    val urlTitle: String = "",
    val usn: Int = 0,
    val createdTime: Long = 0,
    val updatedTime: Long = 0,
    val isDirty: Boolean = false,
)

@Serializable
private data class NotebookBackupDto(
    val localId: Long = 0,
    val serverId: String = "",
    val parentServerId: String = "",
    val title: String = "",
    val seq: Int = 0,
    val usn: Int = 0,
)

@Serializable
private data class SettingsBackupDto(
    val themeMode: String = "SYSTEM",
    val palette: String = "FOREST",
    val dynamicColor: Boolean = false,
    val surfaceEffect: String? = null,
    val surfaceTranslucency: Int? = null,
    val customColors: Map<String, String> = emptyMap(),
    val density: String? = null,
    val noteCardBorder: Boolean? = null,
    val noteCardCorner: Int? = null,
    val uiScale: Int? = null,
    val autoSync: Boolean = true,
    val sortMode: String = "UPDATED_DESC",
    val defaultMarkdown: Boolean = true,
)

@Serializable
private data class ImageBackupDto(
    val localId: String = "",
    val noteLocalId: Long = 0,
    val serverId: String? = null,
    val entryName: String = "",
    val mimeType: String = "image/jpeg",
)

private fun buildHtml(note: NoteEntity): String {
    val title = note.title.ifBlank { "无标题笔记" }
    val body = if (note.isMarkdown) {
        "<pre class=\"markdown\">${escapeHtml(note.content)}</pre>"
    } else {
        extractHtmlBody(note.content)
    }
    return buildString {
        append("<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n")
        append("<meta charset=\"UTF-8\">\n")
        append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
        append("<title>").append(escapeHtml(title)).append("</title>\n")
        append("</head>\n<body>\n").append(body).append("\n</body>\n</html>\n")
    }
}

private fun buildEnex(notes: List<NoteEntity>): String {
    val exportTime = System.currentTimeMillis()
    return buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<!DOCTYPE en-export SYSTEM \"http://xml.evernote.com/pub/en-export.dtd\">\n")
        append("<en-export>\n")
        notes.forEach { note ->
            val created = note.createdTime.takeIf { it > 0 }
                ?: note.updatedTime.takeIf { it > 0 }
                ?: exportTime
            val updated = note.updatedTime.takeIf { it > 0 } ?: created
            val body = if (note.isMarkdown) {
                "<pre>${escapeXml(note.content)}</pre>"
            } else {
                extractHtmlBody(note.content)
            }
            val enml = buildString {
                append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                append("<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">")
                append("<en-note>").append(body).append("</en-note>")
            }
            append("  <note>\n")
            append("    <title>").append(escapeXml(note.title.ifBlank { "无标题笔记" })).append("</title>\n")
            append("    <content><![CDATA[").append(cdata(enml)).append("]]></content>\n")
            note.tagList.forEach { tag ->
                append("    <tag>").append(escapeXml(tag)).append("</tag>\n")
            }
            append("    <created>").append(formatEnexTime(created)).append("</created>\n")
            append("    <updated>").append(formatEnexTime(updated)).append("</updated>\n")
            append("  </note>\n")
        }
        append("</en-export>\n")
    }
}

private fun parseEnex(raw: String): List<ImportedNote> {
    val parser = Xml.newPullParser()
    parser.setInput(StringReader(raw))
    val result = mutableListOf<ImportedNote>()
    var current: ImportedNote? = null
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> {
                val name = parser.name.substringAfter(':')
                val active = current
                if (name == "note" && active == null) {
                    current = ImportedNote()
                } else if (active != null) {
                    when (name) {
                        "title" -> active.title = parser.nextText()
                        "content" -> active.content = parser.nextText()
                        "tag" -> active.tags.add(parser.nextText().trim())
                    }
                }
            }

            XmlPullParser.END_TAG -> {
                val active = current
                if (parser.name.substringAfter(':') == "note" && active != null) {
                    active.tags.removeAll { it.isBlank() }
                    result.add(active)
                    current = null
                }
            }
        }
        event = parser.next()
    }
    return result
}

private fun extractEnoteBody(raw: String): String {
    val content = raw.trim()
    val match = Regex("(?is)<en-note\\b[^>]*>(.*)</en-note\\s*>").find(content)
    if (match != null) return match.groupValues[1].trim()
    if (Regex("(?is)<en-note\\b[^>]*/>").containsMatchIn(content)) return ""
    return content
}

private fun extractHtmlBody(raw: String): String =
    Regex("(?is)<body\\b[^>]*>(.*?)</body\\s*>")
        .find(raw)
        ?.groupValues
        ?.get(1)
        ?.trim()
        ?: raw

private fun fileTitle(displayName: String): String =
    displayName.substringBeforeLast('.').ifBlank { "无标题笔记" }

private fun safeFileBase(title: String): String {
    val cleaned = title
        .replace(Regex("""[\x00-\x1F\\/:*?"<>|]"""), "_")
        .trim()
        .take(80)
        .trim('.', ' ')
    return cleaned.ifBlank { "无标题笔记" }
}

private fun uniqueFileName(base: String, extension: String, usedNames: MutableSet<String>): String {
    var candidate = "$base.$extension"
    var suffix = 2
    while (!usedNames.add(candidate.lowercase(Locale.ROOT))) {
        candidate = "$base ($suffix).$extension"
        suffix++
    }
    return candidate
}

private fun formatEnexTime(millis: Long): String = ENEX_TIME_FORMAT.format(Instant.ofEpochMilli(millis))

private fun cleanXml(value: String): String = value.replace(INVALID_XML_CONTROLS, "")

private fun escapeXml(value: String): String = cleanXml(value)
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")

private fun cdata(value: String): String = cleanXml(value).replace("]]>", "]]]]><![CDATA[>")

private fun escapeHtml(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")

private inline fun <reified T : Enum<T>> enumValueOr(value: String, default: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(default)

private fun Throwable.readableMessage(fallback: String): String = message?.takeIf { it.isNotBlank() } ?: fallback

private fun summarizeErrors(errors: List<String>): String {
    val distinct = errors.distinct()
    if (distinct.isEmpty()) return ""
    val summary = distinct.take(4).joinToString("；")
    return if (distinct.size > 4) "$summary；另有 ${distinct.size - 4} 项错误" else summary
}

package com.leanote.next.data.model

import kotlinx.serialization.Serializable

/**
 * 一个 Leanote 账户。[id] 同时作为本地数据的归属键。
 */
@Serializable
data class Account(
    val id: String,
    val host: String,
    val userId: String,
    val email: String,
    val username: String,
    val token: String,
    val logo: String = "",
    /** 头像版本号，用于让图片缓存感知头像更新。 */
    val avatarVersion: Long = 0L,
    val noteUsn: Int = 0,
    val notebookUsn: Int = 0,
    val lastUsedAt: Long = 0L,
    val lastSyncAt: Long = 0L,
) {
    val displayName: String get() = username.ifBlank { email.substringBefore('@') }

    fun avatarUrl(): String? {
        val raw = logo.trim()
        if (raw.isBlank()) return null
        if (raw.startsWith("data:")) return raw
        if (raw.startsWith("http://") || raw.startsWith("https://")) return withToken(raw)
        val path = raw.trimStart('/')
        // 旧版网页端上传头像时只保存 fileId，需要通过文件接口读取。
        return if (Regex("^[a-fA-F0-9]{24}$").matches(path)) {
            host.trimEnd('/') + "/api/file/getImage?fileId=$path&token=$token"
        } else {
            withToken(host.trimEnd('/') + "/" + path)
        }
    }

    /** Leanote 的文件接口需要 token 鉴权，部分版本返回的头像地址不带 token。 */
    private fun withToken(url: String): String {
        if (!url.contains("/api/file/") || url.contains("token=")) return url
        val separator = if (url.contains('?')) "&" else "?"
        return "$url${separator}token=$token"
    }

    fun blogUrl(): String {
        val owner = username.ifBlank { userId }
        return host.trimEnd('/') + "/blog/" + owner
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** 应用配色方案。 */
enum class ThemePalette { FOREST, OCEAN, LAVENDER, SUNSET, GRAPHITE, CREAM_PAPER, MONOCHROME, NEON }

enum class SortMode { UPDATED_DESC, UPDATED_ASC, CREATED_DESC, TITLE_ASC }

/** 界面材质效果。 */
enum class SurfaceEffect { NONE, FROSTED, ACRYLIC }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val palette: ThemePalette = ThemePalette.FOREST,
    val dynamicColor: Boolean = false,
    val surfaceEffect: SurfaceEffect = SurfaceEffect.NONE,
    /** 材质透明强度，0–60，数值越大越透明。 */
    val surfaceTranslucency: Int = 20,
    val customColors: Map<String, String> = emptyMap(),
    val autoSync: Boolean = true,
    val sortMode: SortMode = SortMode.UPDATED_DESC,
    val defaultMarkdown: Boolean = true,
)

/** 笔记列表的筛选条件 */
data class NoteFilter(
    val notebookServerId: String? = null,
    val tag: String? = null,
    val query: String = "",
    val trash: Boolean = false,
)

enum class EditorKind { MARKDOWN, RICH_TEXT }

class ApiException(message: String, val code: String? = null) : Exception(message)

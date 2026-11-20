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
    val noteUsn: Int = 0,
    val notebookUsn: Int = 0,
    val lastUsedAt: Long = 0L,
    val lastSyncAt: Long = 0L,
) {
    val displayName: String get() = username.ifBlank { email.substringBefore('@') }

    fun avatarUrl(): String? {
        if (logo.isBlank()) return null
        return if (logo.startsWith("http")) logo else host.trimEnd('/') + "/" + logo.trimStart('/')
    }

    fun blogUrl(): String {
        val owner = username.ifBlank { userId }
        return host.trimEnd('/') + "/blog/" + owner
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class SortMode { UPDATED_DESC, UPDATED_ASC, CREATED_DESC, TITLE_ASC }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
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

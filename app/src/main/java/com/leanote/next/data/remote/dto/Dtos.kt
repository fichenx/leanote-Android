package com.leanote.next.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiBase(
    @SerialName("Ok") val ok: Boolean = false,
    @SerialName("Msg") val msg: String? = null,
)

@Serializable
data class LoginResponse(
    @SerialName("Ok") val ok: Boolean = false,
    @SerialName("Msg") val msg: String? = null,
    @SerialName("UserId") val userId: String = "",
    @SerialName("Username") val username: String = "",
    @SerialName("Email") val email: String = "",
    @SerialName("Token") val token: String = "",
)

@Serializable
data class UserResponse(
    @SerialName("Ok") val ok: Boolean = false,
    @SerialName("Msg") val msg: String? = null,
    @SerialName("UserId") val userId: String = "",
    @SerialName("Username") val username: String = "",
    @SerialName("Email") val email: String = "",
    @SerialName("Verified") val verified: Boolean = false,
    @SerialName("Logo") val logo: String = "",
)

@Serializable
data class SyncStateResponse(
    @SerialName("Ok") val ok: Boolean = false,
    @SerialName("Msg") val msg: String? = null,
    @SerialName("LastSyncUsn") val lastSyncUsn: Int = 0,
    @SerialName("LastSyncTime") val lastSyncTime: Long = 0L,
)

@Serializable
data class UpdateResponse(
    @SerialName("Ok") val ok: Boolean = false,
    @SerialName("Msg") val msg: String? = null,
    @SerialName("Usn") val usn: Int = 0,
)

@Serializable
data class NotebookDto(
    @SerialName("Ok") val ok: Boolean = true,
    @SerialName("Msg") val msg: String? = null,
    @SerialName("NotebookId") val notebookId: String = "",
    @SerialName("ParentNotebookId") val parentNotebookId: String? = null,
    @SerialName("UserId") val userId: String = "",
    @SerialName("Title") val title: String = "",
    @SerialName("Seq") val seq: Int = 0,
    @SerialName("IsBlog") val isBlog: Boolean = false,
    @SerialName("IsDeleted") val isDeleted: Boolean = false,
    @SerialName("Usn") val usn: Int = 0,
    @SerialName("UrlTitle") val urlTitle: String = "",
    @SerialName("CreatedTime") val createdTime: String? = null,
    @SerialName("UpdatedTime") val updatedTime: String? = null,
)

@Serializable
data class NoteFileDto(
    @SerialName("FileId") val fileId: String = "",
    @SerialName("LocalFileId") val localFileId: String? = null,
    @SerialName("IsAttach") val isAttach: Boolean = false,
    @SerialName("HasBody") val hasBody: Boolean = false,
    @SerialName("Type") val type: String? = null,
    @SerialName("Title") val title: String? = null,
)

@Serializable
data class NoteDto(
    @SerialName("Ok") val ok: Boolean = true,
    @SerialName("Msg") val msg: String? = null,
    @SerialName("NoteId") val noteId: String = "",
    @SerialName("NotebookId") val notebookId: String = "",
    @SerialName("UserId") val userId: String = "",
    @SerialName("Title") val title: String = "",
    @SerialName("Content") val content: String = "",
    @SerialName("IsMarkdown") val isMarkdown: Boolean = true,
    @SerialName("IsTrash") val isTrash: Boolean = false,
    @SerialName("IsDeleted") val isDeleted: Boolean = false,
    @SerialName("IsBlog") val isBlog: Boolean = false,
    @SerialName("Usn") val usn: Int = 0,
    @SerialName("Tags") val tags: List<String> = emptyList(),
    @SerialName("Files") val files: List<NoteFileDto> = emptyList(),
    @SerialName("UrlTitle") val urlTitle: String = "",
    @SerialName("CreatedTime") val createdTime: String? = null,
    @SerialName("UpdatedTime") val updatedTime: String? = null,
    @SerialName("PublicTime") val publicTime: String? = null,
)

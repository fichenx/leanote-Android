package com.leanote.next.data.remote

import com.leanote.next.data.remote.dto.ApiBase
import com.leanote.next.data.remote.dto.LoginResponse
import com.leanote.next.data.remote.dto.LogoResponse
import com.leanote.next.data.remote.dto.NoteDto
import com.leanote.next.data.remote.dto.NotebookDto
import com.leanote.next.data.remote.dto.SyncStateResponse
import com.leanote.next.data.remote.dto.UpdateResponse
import com.leanote.next.data.remote.dto.UserResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Query

/**
 * Leanote 服务端 API 的 Kotlin 映射。
 * token 显式作为查询参数传递，便于多账户切换。
 *
 * 注意：Kotlin 的 Map<String, RequestBody> 会被编译为 Map<String, ? extends RequestBody>，
 * Retrofit 反射拒绝通配符，因此 @PartMap 参数必须加上 @JvmSuppressWildcards。
 */
interface LeanoteApi {

    @GET("auth/login")
    suspend fun login(
        @Query("email") email: String,
        @Query("pwd") password: String,
    ): LoginResponse

    @GET("auth/register")
    suspend fun register(
        @Query("email") email: String,
        @Query("pwd") password: String,
    ): ApiBase

    @GET("auth/logout")
    suspend fun logout(@Query("token") token: String): ApiBase

    @GET("user/info")
    suspend fun userInfo(
        @Query("userId") userId: String,
        @Query("token") token: String,
    ): UserResponse

    @GET("user/getSyncState")
    suspend fun syncState(@Query("token") token: String): SyncStateResponse

    @Multipart
    @POST("user/updateLogo")
    suspend fun updateLogo(
        @Part file: MultipartBody.Part,
        @Query("token") token: String,
    ): LogoResponse

    @GET("notebook/getSyncNotebooks")
    suspend fun syncNotebooks(
        @Query("afterUsn") afterUsn: Int,
        @Query("maxEntry") maxEntry: Int = 20,
        @Query("token") token: String,
    ): List<NotebookDto>

    @GET("notebook/getNotebooks")
    suspend fun notebooks(@Query("token") token: String): List<NotebookDto>

    @POST("notebook/addNotebook")
    suspend fun addNotebook(
        @Query("title") title: String,
        @Query("parentNotebookId") parentNotebookId: String?,
        @Query("token") token: String,
    ): NotebookDto

    @POST("notebook/updateNotebook")
    suspend fun updateNotebook(
        @Query("notebookId") notebookId: String,
        @Query("title") title: String,
        @Query("parentNotebookId") parentNotebookId: String?,
        @Query("seq") seq: Int,
        @Query("usn") usn: Int,
        @Query("token") token: String,
    ): NotebookDto

    @POST("notebook/deleteNotebook")
    suspend fun deleteNotebook(
        @Query("notebookId") notebookId: String,
        @Query("usn") usn: Int,
        @Query("token") token: String,
    ): UpdateResponse

    @GET("note/getSyncNotes")
    suspend fun syncNotes(
        @Query("afterUsn") afterUsn: Int,
        @Query("maxEntry") maxEntry: Int = 20,
        @Query("token") token: String,
    ): List<NoteDto>

    @GET("note/getNoteAndContent")
    suspend fun noteAndContent(
        @Query("noteId") noteId: String,
        @Query("token") token: String,
    ): NoteDto

    @GET("note/getNotes")
    suspend fun notesOfNotebook(
        @Query("notebookId") notebookId: String,
        @Query("token") token: String,
    ): List<NoteDto>

    @Multipart
    @POST("note/addNote")
    @JvmSuppressWildcards
    suspend fun addNote(
        @PartMap fields: Map<String, RequestBody>,
        @Part files: List<MultipartBody.Part>,
        @Query("token") token: String,
    ): NoteDto

    @Multipart
    @POST("note/updateNote")
    @JvmSuppressWildcards
    suspend fun updateNote(
        @PartMap fields: Map<String, RequestBody>,
        @Part files: List<MultipartBody.Part>,
        @Query("token") token: String,
    ): NoteDto

    @POST("note/deleteTrash")
    suspend fun deleteTrash(
        @Query("noteId") noteId: String,
        @Query("usn") usn: Int,
        @Query("token") token: String,
    ): UpdateResponse

    /**
     * 笔记元数据（不含正文）。Leanote 没有独立的附件列表/上传/删除 API，
     * 附件通过笔记的 Files 字段读取，并通过 note/updateNote 增删。
     */
    @GET("note/getNote")
    suspend fun noteMeta(
        @Query("noteId") noteId: String,
        @Query("token") token: String,
    ): NoteDto
}

package com.leanote.next.data.repo

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.leanote.next.data.local.NoteDao
import com.leanote.next.data.local.NotebookDao
import com.leanote.next.data.model.Account
import com.leanote.next.data.model.ApiException
import com.leanote.next.data.prefs.AccountStore
import com.leanote.next.data.remote.ApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val accountStore: AccountStore,
    private val noteDao: NoteDao,
    private val notebookDao: NotebookDao,
    @ApplicationContext private val context: Context,
) {

    suspend fun login(host: String, email: String, password: String): Account = withContext(Dispatchers.IO) {
        val normalized = ApiClient.normalizeHost(host)
        val api = apiClient.api(normalized)
        val response = runCatching { api.login(email.trim(), password) }
            .getOrElse { throw ApiException("无法连接 $normalized：${it.message ?: "网络错误"}") }
        if (!response.ok || response.token.isBlank()) {
            throw ApiException(response.msg?.takeIf { it.isNotBlank() } ?: "登录失败，请检查邮箱与密码")
        }
        var username = response.username
        var logo = ""
        runCatching { api.userInfo(response.userId, response.token) }.onSuccess {
            if (it.ok) {
                if (it.username.isNotBlank()) username = it.username
                logo = it.logo
            }
        }
        val id = "${response.userId}@$normalized"
        val existing = accountStore.accounts.first().firstOrNull { it.id == id }
        val account = Account(
            id = id,
            host = normalized,
            userId = response.userId,
            email = response.email.ifBlank { email.trim() },
            username = username,
            token = response.token,
            logo = logo,
            noteUsn = existing?.noteUsn ?: 0,
            notebookUsn = existing?.notebookUsn ?: 0,
            lastUsedAt = System.currentTimeMillis(),
            lastSyncAt = existing?.lastSyncAt ?: 0L,
        )
        accountStore.upsert(account, makeActive = true)
        account
    }

    suspend fun register(host: String, email: String, password: String): Account = withContext(Dispatchers.IO) {
        val normalized = ApiClient.normalizeHost(host)
        val api = apiClient.api(normalized)
        val response = runCatching { api.register(email.trim(), password) }
            .getOrElse { throw ApiException("无法连接 $normalized：${it.message ?: "网络错误"}") }
        if (!response.ok) {
            throw ApiException(response.msg?.takeIf { it.isNotBlank() } ?: "注册失败")
        }
        login(normalized, email, password)
    }

    /** 退出登录：通知服务器失效 token，并清理该账户的本地数据。 */
    suspend fun logout(account: Account) = withContext(Dispatchers.IO) {
        runCatching { apiClient.api(account.host).logout(account.token) }
        noteDao.deleteAllOfAccount(account.id)
        notebookDao.deleteAllOfAccount(account.id)
        accountStore.remove(account.id)
    }

    suspend fun removeAccountLocally(account: Account) = withContext(Dispatchers.IO) {
        noteDao.deleteAllOfAccount(account.id)
        notebookDao.deleteAllOfAccount(account.id)
        accountStore.remove(account.id)
    }

    /** 从服务器刷新用户名、邮箱与头像地址。 */
    suspend fun refreshProfile(account: Account): Account? = withContext(Dispatchers.IO) {
        val info = runCatching {
            apiClient.api(account.host).userInfo(account.userId, account.token)
        }.getOrNull() ?: return@withContext null
        if (!info.ok) return@withContext null
        val updated = account.copy(
            username = info.username.ifBlank { account.username },
            email = info.email.ifBlank { account.email },
            logo = info.logo.ifBlank { account.logo },
        )
        if (updated != account) {
            accountStore.upsert(updated, makeActive = false)
        }
        updated
    }

    /** 上传新头像到 Leanote，并同步更新本地账户信息。 */
    suspend fun uploadAvatar(account: Account, uri: Uri): Account = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        if (!mime.startsWith("image/")) throw ApiException("请选择图片文件")
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
        val temp = File(context.cacheDir, "avatar_${UUID.randomUUID()}.$ext")
        runCatching {
            resolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
        }.onFailure {
            temp.delete()
            throw ApiException("读取图片失败")
        }
        if (!temp.isFile || temp.length() == 0L) {
            temp.delete()
            throw ApiException("读取图片失败")
        }
        try {
            val part = MultipartBody.Part.createFormData(
                "file",
                temp.name,
                temp.asRequestBody(mime.toMediaType()),
            )
            val response = runCatching {
                apiClient.api(account.host).updateLogo(part, account.token)
            }.getOrElse {
                throw ApiException("头像上传失败：${it.message ?: "网络错误"}")
            }
            if (response.logo.isBlank()) {
                throw ApiException(response.msg?.takeIf { it.isNotBlank() } ?: "头像上传失败")
            }
            val updated = account.copy(logo = response.logo)
            accountStore.upsert(updated, makeActive = false)
            updated
        } finally {
            temp.delete()
        }
    }
}

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
            if (it.isValid) {
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
    suspend fun refreshProfile(account: Account, forceAvatarRefresh: Boolean = false): Account? = withContext(Dispatchers.IO) {
        val info = runCatching {
            apiClient.api(account.host).userInfo(account.userId, account.token)
        }.getOrElse {
            // 抛出具体原因，便于界面提示是网络不通、地址错误还是 token 失效。
            throw ApiException("无法连接 ${account.host}：${it.message ?: "网络错误"}")
        }
        // /api/user/info 成功时不返回 Ok，只要带回了用户字段就算成功。
        if (!info.isValid) return@withContext null
        val newLogo = info.logo.ifBlank { account.logo }
        val updated = account.copy(
            username = info.username.ifBlank { account.username },
            email = info.email.ifBlank { account.email },
            logo = newLogo,
            // 头像地址变化或手动同步时递增版本号，避免 Coil 命中旧缓存。
            avatarVersion = if (forceAvatarRefresh || newLogo != account.logo) {
                account.avatarVersion + 1
            } else {
                account.avatarVersion
            },
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
            // 部分 Leanote 版本上传成功后只返回 Ok，不返回 Logo，需要再查一次用户信息。
            val info = runCatching {
                apiClient.api(account.host).userInfo(account.userId, account.token)
            }.getOrNull()?.takeIf { it.isValid }
            val serverLogo = response.logo.ifBlank { info?.logo.orEmpty() }
            if (!response.ok && serverLogo.isBlank()) {
                throw ApiException(response.msg?.takeIf { it.isNotBlank() } ?: "头像上传失败")
            }
            val updated = account.copy(
                username = info?.username?.takeIf { it.isNotBlank() } ?: account.username,
                email = info?.email?.takeIf { it.isNotBlank() } ?: account.email,
                logo = serverLogo.ifBlank { account.logo },
                // 即使服务器返回相同地址，也强制刷新一次图片缓存。
                avatarVersion = account.avatarVersion + 1,
            )
            accountStore.upsert(updated, makeActive = false)
            updated
        } finally {
            temp.delete()
        }
    }
}

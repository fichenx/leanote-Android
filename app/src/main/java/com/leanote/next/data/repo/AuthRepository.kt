package com.leanote.next.data.repo

import com.leanote.next.data.local.NoteDao
import com.leanote.next.data.local.NotebookDao
import com.leanote.next.data.model.Account
import com.leanote.next.data.model.ApiException
import com.leanote.next.data.prefs.AccountStore
import com.leanote.next.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val accountStore: AccountStore,
    private val noteDao: NoteDao,
    private val notebookDao: NotebookDao,
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
}

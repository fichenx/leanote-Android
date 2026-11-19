package com.leanote.next.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.leanote.next.data.model.Account
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.accountDataStore: DataStore<Preferences> by preferencesDataStore("leanote_accounts")

@Singleton
class AccountStore @Inject constructor(@ApplicationContext context: Context) {

    private val dataStore = context.accountDataStore
    private val json = Json { ignoreUnknownKeys = true }

    private val accountsKey = stringPreferencesKey("accounts")
    private val activeKey = stringPreferencesKey("active_id")

    val accounts: Flow<List<Account>> = dataStore.data.map { prefs ->
        decode(prefs[accountsKey]).sortedByDescending { it.lastUsedAt }
    }

    val activeAccount: Flow<Account?> = dataStore.data.map { prefs ->
        val all = decode(prefs[accountsKey])
        val id = prefs[activeKey]
        all.firstOrNull { it.id == id } ?: all.maxByOrNull { it.lastUsedAt }
    }

    suspend fun current(): Account? = activeAccount.first()

    suspend fun currentOrThrow(): Account =
        current() ?: throw com.leanote.next.data.model.ApiException("请先登录 Leanote 账户")

    suspend fun upsert(account: Account, makeActive: Boolean = true) {
        dataStore.edit { prefs ->
            val all = decode(prefs[accountsKey]).toMutableList()
            val index = all.indexOfFirst { it.id == account.id }
            if (index >= 0) all[index] = account else all.add(account)
            prefs[accountsKey] = json.encodeToString(all)
            if (makeActive) prefs[activeKey] = account.id
        }
    }

    suspend fun setActive(accountId: String) {
        val all = accounts.first()
        val account = all.firstOrNull { it.id == accountId } ?: return
        upsert(account.copy(lastUsedAt = System.currentTimeMillis()), makeActive = true)
    }

    suspend fun updateUsn(accountId: String, noteUsn: Int, notebookUsn: Int, lastSyncAt: Long) {
        dataStore.edit { prefs ->
            val all = decode(prefs[accountsKey]).toMutableList()
            val index = all.indexOfFirst { it.id == accountId }
            if (index >= 0) {
                all[index] = all[index].copy(
                    noteUsn = noteUsn,
                    notebookUsn = notebookUsn,
                    lastSyncAt = lastSyncAt,
                )
                prefs[accountsKey] = json.encodeToString(all)
            }
        }
    }

    suspend fun remove(accountId: String) {
        dataStore.edit { prefs ->
            val all = decode(prefs[accountsKey]).filterNot { it.id == accountId }
            prefs[accountsKey] = json.encodeToString(all)
            if (prefs[activeKey] == accountId) {
                prefs[activeKey] = all.maxByOrNull { it.lastUsedAt }?.id ?: ""
            }
        }
    }

    private fun decode(raw: String?): List<Account> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<Account>>(raw) }.getOrDefault(emptyList())
    }
}

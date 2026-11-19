package com.leanote.next.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanote.next.data.model.Account
import com.leanote.next.data.prefs.AccountStore
import com.leanote.next.data.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountStore: AccountStore,
) : ViewModel() {

    data class UiState(
        val host: String = "leanote.com",
        val email: String = "",
        val password: String = "",
        val register: Boolean = false,
        val loading: Boolean = false,
        val error: String? = null,
        val showServerField: Boolean = false,
        val accounts: List<Account> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    val loggedIn = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        viewModelScope.launch {
            accountStore.accounts.collect { accounts ->
                _state.update { it.copy(accounts = accounts) }
            }
        }
    }

    fun setHost(value: String) = _state.update { it.copy(host = value, error = null) }

    fun setEmail(value: String) = _state.update { it.copy(email = value, error = null) }

    fun setPassword(value: String) = _state.update { it.copy(password = value, error = null) }

    fun toggleMode() = _state.update { it.copy(register = !it.register, error = null) }

    fun toggleServerField() = _state.update { it.copy(showServerField = !it.showServerField) }

    fun submit() {
        val current = _state.value
        if (current.email.isBlank() || current.password.isBlank()) {
            _state.update { it.copy(error = "请填写邮箱和密码") }
            return
        }
        if (current.password.length < 6) {
            _state.update { it.copy(error = "密码至少 6 位") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                if (current.register) {
                    authRepository.register(current.host, current.email, current.password)
                } else {
                    authRepository.login(current.host, current.email, current.password)
                }
            }.onSuccess {
                _state.update { it.copy(loading = false) }
                loggedIn.emit(Unit)
            }.onFailure { throwable ->
                _state.update {
                    it.copy(loading = false, error = throwable.message ?: "登录失败，请稍后重试")
                }
            }
        }
    }

    fun switchAccount(accountId: String) {
        viewModelScope.launch {
            accountStore.setActive(accountId)
            loggedIn.emit(Unit)
        }
    }
}

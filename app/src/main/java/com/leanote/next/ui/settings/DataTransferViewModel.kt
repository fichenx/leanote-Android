package com.leanote.next.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanote.next.data.local.NoteDao
import com.leanote.next.data.prefs.AccountStore
import com.leanote.next.data.transfer.ExportFormat
import com.leanote.next.data.transfer.NoteTransfer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataTransferViewModel @Inject constructor(
    private val noteTransfer: NoteTransfer,
    private val accountStore: AccountStore,
    private val noteDao: NoteDao,
) : ViewModel() {

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    private val _status = MutableStateFlow("")
    val status = _status.asStateFlow()

    private val _format = MutableStateFlow(ExportFormat.MARKDOWN)
    val format = _format.asStateFlow()

    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    fun selectFormat(format: ExportFormat) {
        _format.value = format
    }

    fun backup(target: Uri) = runOperation("正在备份笔记、设置与图片…") {
        val account = accountStore.current() ?: throw IllegalStateException("请先登录账户")
        val expected = noteDao.observeAll(account.id).first().size
        val count = noteTransfer.backup(target)
        if (expected > 0 && count == 0) {
            "备份失败，请检查目标文件是否可写"
        } else {
            "备份完成，共 $count 篇笔记（含回收站）"
        }
    }

    fun restore(source: Uri) = runOperation("正在从备份恢复…") {
        val (count, error) = noteTransfer.restore(source)
        if (error.isBlank()) {
            "恢复完成，共 $count 篇笔记"
        } else {
            "已恢复 $count 篇笔记；$error"
        }
    }

    fun importFiles(uris: List<Uri>) = runOperation("正在导入所选文件…") {
        val (count, error) = noteTransfer.importFiles(uris, "")
        if (error.isBlank()) {
            "导入完成，共 $count 篇笔记"
        } else {
            "成功导入 $count 篇笔记；$error"
        }
    }

    fun exportAll(targetDir: Uri) = runOperation("正在导出笔记…") {
        val account = accountStore.current() ?: throw IllegalStateException("请先登录账户")
        val notes = noteDao.observeAll(account.id).first().filterNot { it.isTrash }
        if (notes.isEmpty()) {
            "没有可导出的笔记"
        } else {
            val selectedFormat = _format.value
            val count = noteTransfer.exportNotes(notes, targetDir, selectedFormat)
            if (count == notes.size) {
                "导出完成，共 $count 篇笔记（${selectedFormat.label}）"
            } else {
                "已导出 $count/${notes.size} 篇笔记，部分文件未能写入"
            }
        }
    }

    private fun runOperation(progress: String, operation: suspend () -> String) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _status.value = progress
            val result = try {
                operation()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.message?.takeIf { it.isNotBlank() } ?: "操作失败，请稍后重试"
            } finally {
                _busy.value = false
                _status.value = ""
            }
            messages.emit(result)
        }
    }
}

package com.leanote.next.ui.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.leanote.next.MainActivity
import com.leanote.next.data.prefs.AccountStore
import com.leanote.next.data.repo.NoteRepository
import com.leanote.next.data.repo.NotebookRepository
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 接收系统分享的纯文本，保存为一条新笔记后打开它。
 */
@AndroidEntryPoint
class ShareActivity : ComponentActivity() {

    @Inject
    lateinit var accountStore: AccountStore

    @Inject
    lateinit var noteRepository: NoteRepository

    @Inject
    lateinit var notebookRepository: NotebookRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action != Intent.ACTION_SEND) {
            finish()
            return
        }
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        if (text.isEmpty()) {
            Toast.makeText(this, "没有可保存的内容", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim().orEmpty()

        lifecycleScope.launch {
            val account = accountStore.current()
            if (account == null) {
                Toast.makeText(this@ShareActivity, "请先登录 Leanote 账户", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@ShareActivity, MainActivity::class.java))
                finish()
                return@launch
            }
            val notebook = notebookRepository.ensureDefault(account)
            if (notebook == null) {
                Toast.makeText(this@ShareActivity, "无法创建笔记本，请检查网络", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }
            val title = subject.ifBlank { text.lineSequence().firstOrNull()?.take(40).orEmpty() }
            val html = text.split("\n\n").joinToString("") { block ->
                "<p>${block.replace("\n", "<br>").escapeHtml()}</p>"
            }
            val localId = noteRepository.create(
                accountId = account.id,
                notebookServerId = notebook.serverId,
                markdown = false,
                title = title,
                content = html,
                tags = "",
            )
            runCatching { noteRepository.push(localId, account) }
            Toast.makeText(this@ShareActivity, "已保存到「${notebook.title}」", Toast.LENGTH_SHORT).show()
            startActivity(
                Intent(this@ShareActivity, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_NOTE_ID, localId)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
            finish()
        }
    }

    private fun String.escapeHtml(): String = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}

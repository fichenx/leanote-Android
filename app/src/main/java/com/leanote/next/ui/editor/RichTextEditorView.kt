package com.leanote.next.ui.editor

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.leanote.next.data.model.Account
import com.leanote.next.data.repo.ImageRepository
import com.leanote.next.ui.theme.LocalUiScale
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.ByteArrayInputStream

/**
 * 富文本编辑器控制器：包装 WebView 的 contenteditable 区域。
 */
class RichTextController {
    internal var webView: WebView? = null
    internal var pendingHtml: String? = null

    fun exec(command: String, value: String? = null) {
        val arg = value?.let { JSONObject.quote(it) } ?: "null"
        webView?.evaluateJavascript("window.exec(${JSONObject.quote(command)}, $arg)", null)
    }

    fun insertImage(url: String) {
        webView?.evaluateJavascript("window.insertImage(${JSONObject.quote(url)})", null)
    }

    fun insertHtml(html: String) {
        webView?.evaluateJavascript("window.insertHtml(${JSONObject.quote(html)})", null)
    }

    fun getHtml(onResult: (String) -> Unit) {
        val view = webView ?: return onResult("")
        view.evaluateJavascript("window.getHtml()") { raw ->
            onResult(decodeJsString(raw))
        }
    }

    fun focus() {
        webView?.evaluateJavascript("window.focusEditor()", null)
    }

    private fun decodeJsString(raw: String?): String {
        if (raw.isNullOrBlank() || raw == "null") return ""
        return runCatching { JSONObject("{\"v\":$raw}").getString("v") }.getOrDefault("")
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RichTextEditorView(
    initialHtml: String,
    account: Account?,
    images: ImageRepository,
    controller: RichTextController,
    onHtmlChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiScale = LocalUiScale.current
    val bg = MaterialTheme.colorScheme.background.toArgb()
    val fg = MaterialTheme.colorScheme.onSurface.toArgb()
    val link = MaterialTheme.colorScheme.primary.toArgb()
    val quote = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()
    val border = MaterialTheme.colorScheme.outlineVariant.toArgb()

    val document = remember(bg, fg, link, quote, codeBg, border) {
        buildEditorDocument(bg, fg, link, quote, codeBg, border)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.textZoom = uiScale
                isVerticalScrollBarEnabled = false
                addJavascriptInterface(
                    EditorBridge { controller.getHtml { html -> onHtmlChanged(html) } },
                    "LeanoteBridge",
                )
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? {
                        val url = request.url.toString()
                        if (!url.startsWith(LOCAL_HOST)) return null
                        val resolved = runBlocking { images.findLocalImage(url) } ?: return null
                        return WebResourceResponse(
                            resolved.second,
                            null,
                            ByteArrayInputStream(resolved.first.readBytes()),
                        )
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        val html = controller.pendingHtml ?: return
                        controller.pendingHtml = null
                        view.evaluateJavascript("window.setHtml(${JSONObject.quote(html)})", null)
                    }
                }
                loadDataWithBaseURL(account?.host, document, "text/html", "utf-8", null)
                controller.webView = this
                controller.pendingHtml = initialHtml
            }
        },
        update = { webView ->
            webView.settings.textZoom = uiScale
            controller.webView = webView
        },
    )
}

private class EditorBridge(private val onChanged: () -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private var pending = false

    @JavascriptInterface
    fun onContentChanged() {
        if (pending) return
        pending = true
        handler.postDelayed({
            pending = false
            onChanged()
        }, 320)
    }
}

private const val LOCAL_HOST = "https://leanote.local"

private fun hex(color: Int) = String.format("#%06X", 0xFFFFFF and color)

private fun buildEditorDocument(
    background: Int,
    foreground: Int,
    link: Int,
    quote: Int,
    codeBg: Int,
    border: Int,
): String = """
    <!DOCTYPE html>
    <html><head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0">
    <style>
      html, body { margin: 0; padding: 0; background: ${hex(background)}; color: ${hex(foreground)}; }
      body { font-family: -apple-system, "Noto Sans SC", "PingFang SC", Roboto, sans-serif;
             font-size: 17px; line-height: 1.78; -webkit-font-smoothing: antialiased;
             word-wrap: break-word; padding: 0 0 40vh 0; }
      #editor { outline: none; min-height: 70vh; }
      #editor:empty::before { content: "开始记录今天的想法…"; color: ${hex(quote)}88; }
      p { margin: 0 0 16px 0; }
      h1, h2, h3, h4 { line-height: 1.35; margin: 24px 0 12px; font-weight: 600; }
      h1 { font-size: 26px; } h2 { font-size: 22px; } h3 { font-size: 19px; }
      a { color: ${hex(link)}; text-decoration: none; }
      img { max-width: 100% !important; height: auto; border-radius: 12px; display: block; margin: 12px 0; }
      blockquote { margin: 16px 0; padding: 6px 16px; border-left: 3px solid ${hex(link)};
                   color: ${hex(quote)}; background: ${hex(codeBg)}55; border-radius: 0 8px 8px 0; }
      pre, code { font-family: "JetBrains Mono", Menlo, Consolas, monospace; font-size: 14px; }
      pre { background: ${hex(codeBg)}; padding: 14px 16px; border-radius: 12px; overflow-x: auto; }
      code { background: ${hex(codeBg)}; padding: 2px 5px; border-radius: 5px; }
      table { border-collapse: collapse; width: 100%; margin: 16px 0; font-size: 15px; }
      th, td { border: 1px solid ${hex(border)}; padding: 8px 10px; }
      hr { border: none; border-top: 1px solid ${hex(border)}; margin: 24px 0; }
      ul, ol { padding-left: 24px; margin: 0 0 16px 0; }
    </style>
    </head><body>
      <div id="editor" contenteditable="true" spellcheck="false"></div>
      <script>
        (function() {
          var editor = document.getElementById('editor');
          window.setHtml = function(html) { editor.innerHTML = html; };
          window.getHtml = function() { return editor.innerHTML; };
          window.exec = function(command, value) {
            editor.focus();
            document.execCommand(command, false, value);
          };
          window.insertImage = function(url) {
            editor.focus();
            document.execCommand('insertHTML', false, '<img src="' + url + '"><p><br></p>');
          };
          window.insertHtml = function(html) {
            editor.focus();
            document.execCommand('insertHTML', false, html);
          };
          window.focusEditor = function() { editor.focus(); };
          var timer = null;
          editor.addEventListener('input', function() {
            if (timer) clearTimeout(timer);
            timer = setTimeout(function() { LeanoteBridge.onContentChanged(); }, 260);
          });
          editor.addEventListener('blur', function() {
            LeanoteBridge.onContentChanged();
          });
        })();
      </script>
    </body></html>
""".trimIndent()

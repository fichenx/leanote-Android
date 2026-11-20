package com.leanote.next.ui.web

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.leanote.next.data.model.Account
import com.leanote.next.data.repo.ImageRepository
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream

private const val LOCAL_HOST = "https://leanote.local"

/**
 * Markdown 阅读视图（Markwon）。图片地址需事先经过 [ImageRepository.prepareForRender] 处理。
 */
@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val textColor = MaterialTheme.colorScheme.onSurface
    val linkColor = MaterialTheme.colorScheme.primary
    val quoteColor = MaterialTheme.colorScheme.onSurfaceVariant

    val markwon = remember(linkColor, textColor) {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(ImagesPlugin.create())
            .usePlugin(HtmlPlugin.create())
            .usePlugin(object : io.noties.markwon.AbstractMarkwonPlugin() {
                override fun configureTheme(builder: io.noties.markwon.core.MarkwonTheme.Builder) {
                    builder.linkColor(linkColor.toArgb())
                    builder.blockQuoteColor(quoteColor.toArgb())
                }
            })
            .build()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            android.widget.TextView(ctx).apply {
                setTextIsSelectable(true)
                setTextColor(textColor.toArgb())
                setLineSpacing(0f, 1.35f)
                textSize = 16f
                setPadding(0, 0, 0, 0)
                linksClickable = true
            }
        },
        update = { textView ->
            if (textView.tag != markdown) {
                textView.tag = markdown
                markwon.setMarkdown(textView, markdown)
            }
        },
    )
}

/**
 * 富文本（HTML）阅读视图。图片由本地缓存优先加载，远程图片自动补 token。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlContent(
    html: String,
    account: Account?,
    images: ImageRepository,
    modifier: Modifier = Modifier,
    fitToWidth: Boolean = true,
) {
    val context = LocalContext.current
    val bg = MaterialTheme.colorScheme.background.toArgb()
    val fg = MaterialTheme.colorScheme.onSurface.toArgb()
    val link = MaterialTheme.colorScheme.primary.toArgb()
    val quote = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()
    val border = MaterialTheme.colorScheme.outlineVariant.toArgb()

    val fullHtml = remember(html, bg, fg, link, quote, codeBg, border, fitToWidth) {
        buildHtmlDocument(html, bg, fg, link, quote, codeBg, border, fitToWidth)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = !fitToWidth
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = false
                settings.loadsImagesAutomatically = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
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

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        val uri = request.url
                        if (uri.host == "leanote.local") return false
                        runCatching {
                            ContextCompat.startActivity(
                                view.context,
                                android.content.Intent(android.content.Intent.ACTION_VIEW, uri),
                                null,
                            )
                        }
                        return true
                    }
                }
            }
        },
        update = { webView ->
            webView.isHorizontalScrollBarEnabled = !fitToWidth
            if (webView.tag != fullHtml) {
                webView.tag = fullHtml
                webView.loadDataWithBaseURL(account?.host, fullHtml, "text/html", "utf-8", null)
            }
        },
    )
}

internal fun buildHtmlDocument(
    body: String,
    background: Int,
    foreground: Int,
    link: Int,
    quote: Int,
    codeBg: Int,
    border: Int,
    fitToWidth: Boolean = true,
): String {
    fun hex(color: Int) = String.format("#%06X", 0xFFFFFF and color)
    val fitCss = if (fitToWidth) {
        """
          html, body { overflow-x: hidden; }
          body, body * { font-family: -apple-system, "Noto Sans SC", "PingFang SC", Roboto, sans-serif !important; }
          body {
            max-width: 100%; overflow-wrap: anywhere; font-size: 16px !important;
            line-height: 1.9 !important; letter-spacing: 0.1px; padding: 6px 18px 56px 18px !important;
            color: ${hex(foreground)} !important;
          }
          p, div, span, font, li, td, th, blockquote {
            font-size: inherit !important;
            line-height: inherit !important;
          }
          h1, h2, h3, h4, h5, h6 { color: ${hex(foreground)} !important; margin: 1.4em 0 0.7em !important; }
          h1 { font-size: 23px !important; } h2 { font-size: 20px !important; }
          h3 { font-size: 18px !important; } h4 { font-size: 16.5px !important; }
          h5, h6 { font-size: 15px !important; }
          img, video, iframe { max-width: 100% !important; height: auto !important; }
          img { display: block; margin: 16px auto !important; border-radius: 12px; }
          table { display: block; width: 100%; overflow-x: auto; -webkit-overflow-scrolling: touch;
                  font-size: 14px !important; }
          pre { overflow-x: auto; white-space: pre-wrap !important; word-break: break-word; }
          pre, code { font-size: 13.5px !important; }
          blockquote { margin: 16px 0 !important; padding: 8px 14px !important; }
        """
    } else {
        """
          html, body { overflow-x: auto; }
          body { width: max-content; min-width: calc(100% - 36px); padding: 6px 18px 56px 18px; }
          img, video, iframe { max-width: none !important; }
          table { white-space: nowrap; }
          pre { white-space: pre; }
        """
    }
    return """
        <!DOCTYPE html>
        <html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0">
        <style>
          html, body { margin: 0; padding: 0; background: ${hex(background)}; color: ${hex(foreground)}; }
          body { font-family: -apple-system, "Noto Sans SC", "PingFang SC", Roboto, sans-serif;
                 font-size: 17px; line-height: 1.78; padding: 6px 18px 56px 18px;
                 -webkit-font-smoothing: antialiased; word-wrap: break-word; }
          p { margin: 0 0 16px 0; }
          h1, h2, h3, h4 { line-height: 1.35; margin: 24px 0 12px; font-weight: 600; }
          h1 { font-size: 26px; } h2 { font-size: 22px; } h3 { font-size: 19px; }
          h4 { font-size: 17px; }
          a { color: ${hex(link)}; text-decoration: none; }
          img { height: auto; border-radius: 12px; display: block; margin: 12px auto; }
          blockquote { margin: 16px 0; padding: 6px 16px; border-left: 3px solid ${hex(link)};
                       color: ${hex(quote)}; background: ${hex(codeBg)}55; border-radius: 0 8px 8px 0; }
          pre, code { font-family: "JetBrains Mono", Menlo, Consolas, monospace; font-size: 14px; }
          pre { background: ${hex(codeBg)}; padding: 14px 16px; border-radius: 12px; }
          code { background: ${hex(codeBg)}; padding: 2px 5px; border-radius: 5px; }
          pre code { background: transparent; padding: 0; }
          table { border-collapse: collapse; width: 100%; margin: 16px 0; font-size: 15px; }
          th, td { border: 1px solid ${hex(border)}; padding: 8px 10px; text-align: left; }
          th { background: ${hex(codeBg)}; }
          hr { border: none; border-top: 1px solid ${hex(border)}; margin: 24px 0; }
          ul, ol { padding-left: 24px; margin: 0 0 16px 0; }
          li { margin: 4px 0; }
          iframe, video { border-radius: 12px; }
          .leanote-image-wrapper { margin: 12px 0; }
          $fitCss
        </style>
        </head><body>$body</body></html>
    """.trimIndent()
}

package com.leanote.next.util

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object TimeUtils {

    private val serverFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .withZone(ZoneOffset.UTC)

    fun parseServerTime(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        runCatching { return OffsetDateTime.parse(raw).toInstant().toEpochMilli() }
        runCatching { return Instant.parse(raw).toEpochMilli() }
        runCatching {
            return OffsetDateTime.parse(raw.replace(' ', 'T') + "Z").toInstant().toEpochMilli()
        }
        return 0L
    }

    fun toServerTime(millis: Long): String = serverFormat.format(Instant.ofEpochMilli(millis))

    fun formatFull(millis: Long): String {
        if (millis <= 0) return ""
        val fmt = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA)
        return fmt.format(Date(millis))
    }

    fun formatList(millis: Long): String {
        if (millis <= 0) return ""
        val now = System.currentTimeMillis()
        val diff = now - millis
        if (diff < 60_000) return "刚刚"
        if (diff < 3_600_000) return "${diff / 60_000} 分钟前"
        val zone = ZoneId.systemDefault()
        val time = Instant.ofEpochMilli(millis).atZone(zone)
        val today = LocalDate.now(zone)
        val hm = "%02d:%02d".format(time.hour, time.minute)
        return when (time.toLocalDate()) {
            today -> "今天 $hm"
            today.minusDays(1) -> "昨天 $hm"
            else -> {
                if (time.year == today.year) "${time.monthValue}月${time.dayOfMonth}日"
                else "${time.year}年${time.monthValue}月${time.dayOfMonth}日"
            }
        }
    }
}

object HtmlUtils {
    private val tagRegex = Regex("<[^>]+>")
    private val styleRegex = Regex("(?is)<(script|style)[^>]*>.*?</\\1>")
    private val spaceRegex = Regex("\\s+")

    fun toPlainText(html: String): String {
        var text = styleRegex.replace(html, " ")
        text = text.replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n")
            .replace(Regex("(?i)</div>"), "\n")
            .replace(tagRegex, " ")
        text = text.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
        return text.replace(spaceRegex, " ").trim()
    }

    fun snippet(raw: String, markdown: Boolean, max: Int = 160): String {
        val text = if (markdown) {
            raw.replace(Regex("!\\[[^]]*]\\([^)]*\\)"), " ")
                .replace(Regex("\\[([^]]*)]\\([^)]*\\)"), "$1")
                .replace(Regex("[#>*`_~|-]"), " ")
                .replace(Regex("\\s+"), " ")
        } else {
            toPlainText(raw)
        }
        return if (text.length <= max) text.trim() else text.take(max).trim() + "…"
    }

    fun titleOrPlaceholder(title: String, content: String, markdown: Boolean): String {
        if (title.isNotBlank()) return title
        val firstLine = if (markdown) {
            content.lineSequence().firstOrNull { it.isNotBlank() }?.replace(Regex("^[#>*\\-\\s]+"), "") ?: ""
        } else {
            toPlainText(content)
        }
        return firstLine.take(40).ifBlank { "无标题笔记" }
    }
}

object ImageRefs {
    val localPattern = Regex("""file:/+getImage\?id=([A-Za-z0-9_\-]+)""")
    val remotePattern = Regex(
        """(https?://[^"'\s()<>]+?/api/file/getImage\?fileId=([A-Za-z0-9_\-]+))(?:&token=[^"'\s()<>]*)?"""
    )

    fun localIds(content: String): List<String> =
        localPattern.findAll(content).map { it.groupValues[1] }.distinct().toList()

    fun replaceLocalIds(content: String, mapping: Map<String, String>): String =
        localPattern.replace(content) { match ->
            mapping[match.groupValues[1]] ?: match.value
        }
}

package com.example.data.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Result of retrieving publicly available YouTube video metadata (title, description, thumbnail).
 * Never invents title or description when metadata retrieval fails or is unavailable offline.
 */
data class YouTubeFetchedMetadata(
    val videoId: String,
    val canonicalUrl: String,
    val isShort: Boolean,
    val thumbnailUrl: String,
    val maxResThumbnailUrl: String,
    val title: String? = null,
    val description: String? = null,
    val authorName: String? = null,
    val metadataAvailable: Boolean = false
)

/**
 * Lightweight, dependency-free YouTube metadata retriever for Admin YouTube Direct Posts
 * and Highlights previews.
 *
 * Strategy:
 * 1. Official keyless YouTube oEmbed JSON endpoint for original title, author, and thumbnail.
 * 2. Bounded YouTube watch page meta-tag inspection (`og:description` / `description`) for
 *    original description when available.
 * 3. If network is offline or retrieval fails, returns clean fallback preview metadata
 *    (`title = null`, `description = null`, `metadataAvailable = false`) without inventing content.
 */
object YouTubeMetadataFetcher {

    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val READ_TIMEOUT_MS = 5_000
    private const val MAX_HTML_BYTES = 256 * 1024

    /**
     * Injectable HTTP GET hook so Robolectric/JVM unit tests can deterministically verify
     * both metadata-available and metadata-unavailable fallback flows without live network I/O.
     */
    @Volatile
    internal var httpGetProvider: (String) -> String? = ::defaultHttpGet

    suspend fun fetchMetadata(url: String): YouTubeFetchedMetadata? = withContext(Dispatchers.IO) {
        val parsed = YouTubeMediaHelper.parse(url) ?: return@withContext null
        val fallback = YouTubeFetchedMetadata(
            videoId = parsed.videoId,
            canonicalUrl = parsed.canonicalWatchUrl,
            isShort = parsed.isShort,
            thumbnailUrl = parsed.thumbnailUrl,
            maxResThumbnailUrl = parsed.maxResThumbnailUrl,
            title = null,
            description = null,
            authorName = null,
            metadataAvailable = false
        )

        var title: String? = null
        var authorName: String? = null
        var thumbnailUrl = parsed.thumbnailUrl
        var description: String? = null

        // 1. Keyless YouTube oEmbed endpoint
        val encodedUrl = runCatching { URLEncoder.encode(parsed.canonicalWatchUrl, "UTF-8") }
            .getOrDefault(parsed.canonicalWatchUrl)
        val oEmbedUrl = "https://www.youtube.com/oembed?url=$encodedUrl&format=json"
        val oEmbedBody = runCatching { httpGetProvider(oEmbedUrl) }.getOrNull()
        if (!oEmbedBody.isNullOrBlank()) {
            runCatching {
                val json = JSONObject(oEmbedBody)
                title = json.optString("title", "").trim().takeIf { it.isNotEmpty() }
                authorName = json.optString("author_name", "").trim().takeIf { it.isNotEmpty() }
                val oThumb = json.optString("thumbnail_url", "").trim()
                if (oThumb.startsWith("https://")) {
                    thumbnailUrl = oThumb
                }
            }
        }

        // 2. Watch page OpenGraph/meta description (and title fallback if oEmbed was blocked)
        val watchUrl = "https://www.youtube.com/watch?v=${parsed.videoId}"
        val html = runCatching { httpGetProvider(watchUrl) }.getOrNull()
        if (!html.isNullOrBlank()) {
            if (title.isNullOrBlank()) {
                title = extractMetaContent(html, "property", "og:title")
                    ?: extractMetaContent(html, "name", "title")
            }
            description = extractMetaContent(html, "property", "og:description")
                ?: extractMetaContent(html, "name", "description")
        }

        val hasAnyMetadata = !title.isNullOrBlank() || !description.isNullOrBlank()
        fallback.copy(
            thumbnailUrl = thumbnailUrl,
            title = title,
            description = description,
            authorName = authorName,
            metadataAvailable = hasAnyMetadata
        )
    }

    internal fun extractMetaContent(html: String, attrName: String, attrValue: String): String? {
        val pattern1 = Regex(
            """<meta\b[^>]*\b$attrName\s*=\s*["']${Regex.escape(attrValue)}["'][^>]*\bcontent\s*=\s*["']([^"']*)["'][^>]*>""",
            setOf(RegexOption.IGNORE_CASE)
        )
        val pattern2 = Regex(
            """<meta\b[^>]*\bcontent\s*=\s*["']([^"']*)["'][^>]*\b$attrName\s*=\s*["']${Regex.escape(attrValue)}["'][^>]*>""",
            setOf(RegexOption.IGNORE_CASE)
        )
        val raw = pattern1.find(html)?.groupValues?.getOrNull(1)
            ?: pattern2.find(html)?.groupValues?.getOrNull(1)
            ?: return null
        return unescapeHtmlEntities(raw).trim().takeIf { it.isNotEmpty() }
    }

    internal fun unescapeHtmlEntities(input: String): String {
        val numericReplaced = Regex("&#(x?[0-9a-fA-F]+);").replace(input) { match ->
            val token = match.groupValues[1]
            val codePoint = runCatching {
                if (token.startsWith("x", ignoreCase = true)) {
                    token.substring(1).toInt(16)
                } else {
                    token.toInt(10)
                }
            }.getOrNull()
            if (codePoint != null && codePoint in 32..0x10FFFF) {
                String(Character.toChars(codePoint))
            } else {
                match.value
            }
        }
        return numericReplaced
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    private fun defaultHttpGet(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as? HttpURLConnection) ?: return null
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
            )
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")

            val code = connection.responseCode
            if (code !in 200..299) return null

            BufferedInputStream(connection.inputStream).use { input ->
                val buffer = ByteArray(8192)
                val out = ByteArrayOutputStream()
                var total = 0
                while (total < MAX_HTML_BYTES) {
                    val read = input.read(buffer, 0, minOf(buffer.size, MAX_HTML_BYTES - total))
                    if (read <= 0) break
                    out.write(buffer, 0, read)
                    total += read
                }
                out.toString(Charsets.UTF_8.name())
            }
        } catch (_: Throwable) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}

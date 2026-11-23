package com.example.data.media

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Parsed YouTube metadata for direct video posts, custom video posts, and 24-hour Shorts highlights.
 */
data class ParsedYouTubeVideo(
    val videoId: String,
    val originalUrl: String,
    val isShort: Boolean,
    val canonicalWatchUrl: String,
    val externalFallbackUrl: String,
    val thumbnailUrl: String,
    val maxResThumbnailUrl: String,
    val clickToPlayEmbedUrl: String,
    val staticEmbedUrl: String
)

/**
 * Pure, dependency-free YouTube URL parser and embed/thumbnail helper.
 * Supports:
 * - youtube.com/watch?v=...
 * - youtu.be/...
 * - youtube.com/shorts/...
 * - youtube.com/embed/...
 * - youtube.com/live/...
 */
object YouTubeMediaHelper {

    const val PRIVACY_ENHANCED_EMBED_HOST = "https://www.youtube-nocookie.com"

    /**
     * JavaScript snippet that pauses any direct HTML5 <video> element and dispatches the
     * YouTube IFrame Player API pauseVideo command specifically to the privacy-enhanced
     * YouTube embed target origin.
     */
    const val PAUSE_EMBED_PLAYER_JS: String =
        "(function(){" +
            "var videos=document.querySelectorAll('video');" +
            "for(var i=0;i<videos.length;i++){try{videos[i].pause();}catch(e){}}" +
            "var f=document.getElementById('yt-player')||document.querySelector('iframe');" +
            "if(f&&f.contentWindow){" +
                "try{f.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"pauseVideo\",\"args\":\"\"}','$PRIVACY_ENHANCED_EMBED_HOST');}catch(e){}" +
            "}" +
        "})();"

    private val VIDEO_ID_REGEX = Regex("^[A-Za-z0-9_-]{11}$")
    private val HOST_LABEL_REGEX = Regex("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$")

    private val ALLOWED_YOUTUBE_HOSTS = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "youtube-nocookie.com",
        "www.youtube-nocookie.com"
    )

    private val ALLOWED_SHORT_HOSTS = setOf(
        "youtu.be",
        "www.youtu.be"
    )

    fun isValidYouTubeUrl(url: String?): Boolean = parse(url) != null

    fun isValidYouTubeShortsUrl(url: String?): Boolean = parse(url)?.isShort == true

    fun extractVideoId(url: String?): String? = parse(url)?.videoId

    /**
     * Builds an app-scoped HTTPS document origin from the Android package name for use as the
     * WebView baseUrl and matching YouTube IFrame API origin parameter.
     */
    fun appDocumentOrigin(packageName: String?): String? {
        val candidate = packageName?.trim()?.lowercase()?.replace('_', '-') ?: return null
        if (candidate.isEmpty() || !candidate.contains('.')) return null
        return normalizeEmbedOrigin("https://$candidate")
    }

    /**
     * Extracts and validates an HTTP(S) embedding document origin (scheme://host[:port]) from a
     * WebView baseUrl. Returns null if the URL is null/blank, uses a non-HTTP(S) scheme, has an
     * invalid host, or points to YouTube's own embed/video domains (which must not be hardcoded
     * as the embedding document origin).
     */
    fun normalizeEmbedOrigin(baseUrl: String?): String? {
        val trimmed = baseUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null

        val host = uri.host?.lowercase()?.trimEnd('.') ?: return null
        if (host.isEmpty() || !host.contains('.')) return null
        val labels = host.split('.')
        if (labels.size < 2 || labels.any { !HOST_LABEL_REGEX.matches(it) }) return null

        if (isYouTubeHost(host)) return null

        val port = uri.port
        val isDefaultPort = port == -1 || (scheme == "http" && port == 80) || (scheme == "https" && port == 443)
        return if (isDefaultPort) {
            "$scheme://$host"
        } else if (port in 1..65535) {
            "$scheme://$host:$port"
        } else {
            null
        }
    }

    private fun isYouTubeHost(host: String): Boolean =
        host in ALLOWED_YOUTUBE_HOSTS ||
            host in ALLOWED_SHORT_HOSTS ||
            host.endsWith(".youtube.com") ||
            host.endsWith(".youtube-nocookie.com") ||
            host.endsWith(".youtu.be")

    fun parse(url: String?): ParsedYouTubeVideo? {
        val trimmed = url?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "https" && scheme != "http") return null

        val host = uri.host?.lowercase() ?: return null
        val segments = (uri.rawPath ?: "")
            .split('/')
            .filter { it.isNotBlank() }

        var videoId: String? = null
        var isShort = false

        when {
            host in ALLOWED_SHORT_HOSTS -> {
                videoId = segments.firstOrNull()
            }
            host in ALLOWED_YOUTUBE_HOSTS -> {
                val first = segments.firstOrNull()?.lowercase()
                when (first) {
                    "watch" -> {
                        videoId = extractQueryParam(uri.rawQuery, "v")
                    }
                    "shorts" -> {
                        videoId = segments.getOrNull(1)
                        isShort = true
                    }
                    "embed" -> {
                        videoId = segments.getOrNull(1)
                    }
                    "live" -> {
                        videoId = segments.getOrNull(1)
                    }
                    else -> return null
                }
            }
            else -> return null
        }

        val cleanId = videoId?.trim() ?: return null
        if (!VIDEO_ID_REGEX.matches(cleanId)) return null

        val canonicalWatchUrl = if (isShort) {
            "https://www.youtube.com/shorts/$cleanId"
        } else {
            "https://www.youtube.com/watch?v=$cleanId"
        }

        return ParsedYouTubeVideo(
            videoId = cleanId,
            originalUrl = trimmed,
            isShort = isShort,
            canonicalWatchUrl = canonicalWatchUrl,
            externalFallbackUrl = canonicalWatchUrl,
            thumbnailUrl = thumbnailUrl(cleanId),
            maxResThumbnailUrl = maxResThumbnailUrl(cleanId),
            clickToPlayEmbedUrl = clickToPlayEmbedUrl(cleanId),
            staticEmbedUrl = staticEmbedUrl(cleanId)
        )
    }

    fun thumbnailUrl(videoId: String): String =
        "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

    fun maxResThumbnailUrl(videoId: String): String =
        "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"

    fun clickToPlayEmbedUrl(videoId: String, embedOrigin: String? = null): String =
        buildEmbedUrl(videoId = videoId, autoplay = true, embedOrigin = embedOrigin)

    fun staticEmbedUrl(videoId: String, embedOrigin: String? = null): String =
        buildEmbedUrl(videoId = videoId, autoplay = false, embedOrigin = embedOrigin)

    private fun buildEmbedUrl(videoId: String, autoplay: Boolean, embedOrigin: String?): String {
        val params = buildString {
            if (autoplay) {
                append("autoplay=1&")
            }
            append("rel=0&modestbranding=1&playsinline=1&enablejsapi=1")
            val validOrigin = normalizeEmbedOrigin(embedOrigin)
            if (validOrigin != null) {
                append("&origin=")
                append(URLEncoder.encode(validOrigin, "UTF-8"))
            }
        }
        return "$PRIVACY_ENHANCED_EMBED_HOST/embed/$videoId?$params"
    }

    /**
     * Generates a minimal responsive HTML document for YouTube privacy-enhanced WebView embedding.
     * When [documentBaseUrl] resolves to a valid non-YouTube HTTP(S) origin matching the WebView's
     * loadDataWithBaseURL baseUrl, the IFrame API `origin` query parameter is configured to match.
     */
    fun buildClickToPlayEmbedHtml(
        videoId: String,
        autoplay: Boolean = true,
        documentBaseUrl: String? = null
    ): String? {
        if (!VIDEO_ID_REGEX.matches(videoId)) return null
        val src = buildEmbedUrl(videoId = videoId, autoplay = autoplay, embedOrigin = documentBaseUrl)
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0" />
              <meta name="referrer" content="strict-origin-when-cross-origin" />
              <style>
                html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; }
                iframe { width: 100%; height: 100%; border: 0; display: block; }
              </style>
            </head>
            <body>
              <iframe
                id="yt-player"
                src="$src"
                title="YouTube video player"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                referrerpolicy="strict-origin-when-cross-origin"
                allowfullscreen>
              </iframe>
            </body>
            </html>
        """.trimIndent()
    }

    private fun extractQueryParam(rawQuery: String?, key: String): String? {
        if (rawQuery.isNullOrBlank()) return null
        for (pair in rawQuery.split('&')) {
            val idx = pair.indexOf('=')
            val rawName = if (idx >= 0) pair.substring(0, idx) else pair
            val name = runCatching { URLDecoder.decode(rawName, "UTF-8") }.getOrDefault(rawName)
            if (name == key && idx >= 0) {
                val rawValue = pair.substring(idx + 1)
                return runCatching { URLDecoder.decode(rawValue, "UTF-8") }.getOrDefault(rawValue)
            }
        }
        return null
    }
}

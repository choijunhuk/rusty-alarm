package com.example.rustyalarm.ui.components

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * YouTube IFrame Player wrapped in a WebView so an alarm can wake the user
 * with a chosen video, Short, or playlist. Uses the official IFrame embed
 * path — no SDK required.
 *
 * Accepted URL forms:
 *  - https://www.youtube.com/watch?v=VIDEO_ID(&t=42|&start=42)
 *  - https://youtu.be/VIDEO_ID(?t=42)
 *  - https://www.youtube.com/shorts/VIDEO_ID
 *  - https://www.youtube.com/playlist?list=PLAYLIST_ID
 *  - https://music.youtube.com/playlist?list=PLAYLIST_ID
 *
 * Invalid URLs surface an inline error card instead of disappearing silently.
 * Embeds that fail (videos blocked by uploader) show a fallback message.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerCard(
    url: String,
    modifier: Modifier = Modifier,
) {
    val parsed = remember(url) { parseYouTube(url) }

    if (parsed == null) {
        YouTubeErrorCard(
            modifier = modifier,
            text = "YouTube URL 형식이 올바르지 않아요.",
        )
        return
    }

    val html = remember(parsed) { buildIframeHtml(parsed) }
    var loadFailed by remember(url) { mutableStateOf(false) }

    if (loadFailed) {
        YouTubeErrorCard(
            modifier = modifier,
            text = "이 영상은 외부에서 재생할 수 없어요. YouTube 앱에서 열어보세요.",
        )
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            val webRef = remember { mutableStateOf<WebView?>(null) }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webRef.value = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?,
                            ) {
                                if (request?.isForMainFrame == true) loadFailed = true
                            }
                            override fun onPageFinished(view: WebView, url: String?) {
                                // Force playVideo via the IFrame API in case
                                // autoplay was blocked by the WebView heuristic.
                                view.evaluateJavascript(
                                    "if (window.__forcePlay) window.__forcePlay();",
                                    null,
                                )
                            }
                        }
                        setBackgroundColor(android.graphics.Color.BLACK)
                        loadDataWithBaseURL(
                            "https://www.youtube.com",
                            html,
                            "text/html",
                            "utf-8",
                            null,
                        )
                    }
                },
            )
            DisposableEffect(Unit) {
                onDispose {
                    runCatching {
                        webRef.value?.let { web ->
                            web.onPause()
                            web.stopLoading()
                            web.loadUrl("about:blank")
                            web.removeAllViews()
                            web.destroy()
                        }
                        webRef.value = null
                    }
                }
            }
        }
    }
}

@Composable
private fun YouTubeErrorCard(modifier: Modifier, text: String) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Warning, null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

/**
 * Small static thumbnail (no playback) for showing a preview of the chosen
 * video on the alarm edit screen. Returns null if the URL is unparseable.
 */
fun youtubeThumbnailUrl(url: String): String? {
    val ref = parseYouTube(url) ?: return null
    val id = ref.videoId ?: return null
    return "https://img.youtube.com/vi/$id/hqdefault.jpg"
}

private data class YouTubeRef(
    val videoId: String? = null,
    val playlistId: String? = null,
    val startSeconds: Int = 0,
)

// Strict allowlist — every YouTube id is opaque [A-Za-z0-9_-]; reject anything else.
private val ID_PATTERN = Regex("^[A-Za-z0-9_-]{1,64}$")

private fun safeId(raw: String?): String? =
    raw?.takeIf { ID_PATTERN.matches(it) }

private fun isYouTubeHost(host: String): Boolean =
    host == "youtube.com" ||
        host == "music.youtube.com" ||
        host.endsWith(".youtube.com") ||
        host == "youtu.be"

/** Parses `1h2m3s`, `42s`, or `42` into seconds. Returns 0 on garbage. */
private fun parseTimestamp(raw: String?): Int {
    if (raw.isNullOrBlank()) return 0
    val plain = raw.toIntOrNull()
    if (plain != null) return plain.coerceAtLeast(0)
    val m = Regex("(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?").matchEntire(raw) ?: return 0
    val h = m.groupValues[1].toIntOrNull() ?: 0
    val mn = m.groupValues[2].toIntOrNull() ?: 0
    val s = m.groupValues[3].toIntOrNull() ?: 0
    return (h * 3600 + mn * 60 + s).coerceAtLeast(0)
}

private fun parseYouTube(url: String): YouTubeRef? {
    val u = runCatching { Uri.parse(url.trim()) }.getOrNull() ?: return null
    val host = u.host?.lowercase() ?: return null
    if (!isYouTubeHost(host)) return null

    val list = safeId(u.getQueryParameter("list"))
    val tParam = u.getQueryParameter("t") ?: u.getQueryParameter("start")
    val startSec = parseTimestamp(tParam)

    if (host == "youtu.be") {
        val id = safeId(u.lastPathSegment)
        if (id == null && list == null) return null
        return YouTubeRef(videoId = id, playlistId = list, startSeconds = startSec)
    }

    // youtube.com/shorts/{id}
    val segments = u.pathSegments
    if (segments.size >= 2 && segments[0].equals("shorts", ignoreCase = true)) {
        val id = safeId(segments[1])
        if (id != null) return YouTubeRef(videoId = id, startSeconds = startSec)
    }

    val v = safeId(u.getQueryParameter("v"))
    if (v != null || list != null) return YouTubeRef(videoId = v, playlistId = list, startSeconds = startSec)

    return null
}

private fun buildIframeHtml(ref: YouTubeRef): String {
    val startQ = if (ref.startSeconds > 0) "&start=${ref.startSeconds}" else ""
    val src = when {
        ref.playlistId != null && ref.videoId != null ->
            "https://www.youtube.com/embed/${ref.videoId}?list=${ref.playlistId}&autoplay=1&playsinline=1$startQ&enablejsapi=1"
        ref.playlistId != null ->
            "https://www.youtube.com/embed/videoseries?list=${ref.playlistId}&autoplay=1&playsinline=1$startQ&enablejsapi=1"
        ref.videoId != null ->
            "https://www.youtube.com/embed/${ref.videoId}?autoplay=1&playsinline=1&loop=1&playlist=${ref.videoId}$startQ&enablejsapi=1"
        else -> return ""
    }
    return """
        <!doctype html>
        <html><head>
          <meta name='viewport' content='width=device-width, initial-scale=1, user-scalable=no'/>
          <style>
            html,body{margin:0;padding:0;background:#000;height:100%;width:100%;overflow:hidden}
            iframe{width:100%;height:100%;border:0;display:block}
          </style>
        </head><body>
          <iframe id='yt' allow='autoplay; fullscreen; encrypted-media; accelerometer; gyroscope'
            allowfullscreen
            src='$src'></iframe>
          <script>
            window.__forcePlay = function () {
              try {
                var f = document.getElementById('yt');
                if (f && f.contentWindow) {
                  f.contentWindow.postMessage(
                    '{"event":"command","func":"playVideo","args":""}',
                    'https://www.youtube.com'
                  );
                }
              } catch (e) {}
            };
            window.addEventListener('load', function () { setTimeout(window.__forcePlay, 700); });
          </script>
        </body></html>
    """.trimIndent()
}

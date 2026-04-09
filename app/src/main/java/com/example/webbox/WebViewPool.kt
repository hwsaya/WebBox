package com.example.webbox

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.util.LruCache
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView

object WebViewPool {

    var isLruEnabled = false  // Matches PrefsHelper default
        private set

    private var currentMaxSize = 3
    private val snapshots = HashMap<String, Bitmap>()
    private val themeMap = HashMap<String, Boolean>()

    private var webViewCache = object : LruCache<String, WebView>(currentMaxSize.coerceAtLeast(1)) {
        override fun entryRemoved(evicted: Boolean, key: String, oldValue: WebView, newValue: WebView?) {
            super.entryRemoved(evicted, key, oldValue, newValue)
            if (oldValue != newValue) oldValue.destroyWebView()
        }
    }

    fun getOrCreateWebView(context: Context, url: String, isDark: Boolean): WebView {
        var webView = webViewCache.get(url)

        // Theme changed — rebuild WebView to apply new dark mode setting
        if (webView != null && themeMap[url] != isDark) {
            remove(url)
            webView = null
        }

        if (webView != null) {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.onResume()
        } else {
            webView = createWebView(context, url, isDark)
            themeMap[url] = isDark
            webViewCache.put(url, webView)
        }
        return webView
    }

    fun onRelease(url: String, webView: WebView) {
        if (isLruEnabled) {
            webView.onPause()
            val cached = webViewCache.get(url)
            when {
                cached == null -> webViewCache.put(url, webView)
                // A newer instance was injected (e.g. theme rebuild) — discard this stale one
                cached != webView -> webView.destroyWebView()
            }
        } else {
            remove(url)
        }
    }

    fun captureSnapshot(url: String) {
        val webView = webViewCache.get(url) ?: return
        val width = webView.width
        val height = webView.height
        if (width <= 0 || height <= 0) return
        try {
            val scale = 0.3f
            val dw = (width * scale).toInt()
            val dh = (height * scale).toInt()
            if (dw <= 0 || dh <= 0) return
            val bitmap = Bitmap.createBitmap(dw, dh, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).also { it.scale(scale, scale); webView.draw(it) }
            snapshots.put(url, bitmap)?.recycle()  // Recycle old bitmap to prevent memory leak
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSnapshot(url: String): Bitmap? = snapshots[url]
    fun getCachedUrls(): List<String> = webViewCache.snapshot().keys.toList()

    fun updateConfig(enabled: Boolean, maxSize: Int) {
        isLruEnabled = enabled
        val targetSize = if (enabled) maxSize.coerceAtLeast(1) else 1
        if (targetSize != currentMaxSize) {
            currentMaxSize = targetSize
            webViewCache.resize(targetSize)
        }
    }

    fun remove(url: String) {
        webViewCache.remove(url)
        snapshots.remove(url)?.recycle()
        themeMap.remove(url)
    }

    private fun WebView.destroyWebView() {
        try {
            (parent as? ViewGroup)?.removeView(this)
            stopLoading()
            clearHistory()
            removeAllViews()
            destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createWebView(context: Context, url: String, isDark: Boolean): WebView {
        return WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    isAlgorithmicDarkeningAllowed = isDark
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    forceDark = if (isDark) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
                }
            }
            loadUrl(url)
        }
    }
}

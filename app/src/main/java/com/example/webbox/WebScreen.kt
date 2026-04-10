package com.example.webbox

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import java.net.URLEncoder

@Composable
fun WebScreen(url: String, navController: NavController, viewModel: WebViewModel) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    val webView = remember(url, isDarkTheme) {
        WebViewPool.getOrCreateWebView(context, url, isDarkTheme)
    }

    val siteName = remember(url) {
        viewModel.webSites.find { it.url == url }?.name ?: "WebBox"
    }

    var canGoBack by remember { mutableStateOf(false) }
    var isBackStarted by remember { mutableStateOf(false) }
    var showTabSwitcher by remember { mutableStateOf(false) }
    var cachedTabCount by remember { mutableIntStateOf(1) }

    val defaultUserAgent = remember { android.webkit.WebSettings.getDefaultUserAgent(context) }
    var isDesktopMode by remember(url) { mutableStateOf(false) }
    val desktopModeState = rememberUpdatedState(isDesktopMode)

    val desktopJs = """
        (function() {
            try {
                var meta = document.querySelector('meta[name="viewport"]');
                var content = 'width=1200, initial-scale=0.1, maximum-scale=5.0, user-scalable=yes';
                if (meta) {
                    if (meta.getAttribute('content') !== content) meta.setAttribute('content', content);
                } else if (document.head) {
                    meta = document.createElement('meta');
                    meta.name = 'viewport';
                    meta.content = content;
                    document.head.appendChild(meta);
                }
                if (document.documentElement) {
                    document.documentElement.style.minWidth = '1200px';
                }
            } catch(e) {}
        })();
    """.trimIndent()

    val mainContentScale by animateFloatAsState(
        targetValue = if (showTabSwitcher) 0.85f else 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing), label = ""
    )
    val mainContentCorner by animateDpAsState(
        targetValue = if (showTabSwitcher || isBackStarted) 24.dp else 0.dp,
        animationSpec = tween(250), label = ""
    )

    DisposableEffect(url) {
        cachedTabCount = WebViewPool.getCachedUrls().size.coerceAtLeast(1)
        onDispose { viewModel.enqueueAutoBackup() }
    }

    PredictiveBackHandler(enabled = !showTabSwitcher) { progressFlow ->
        try {
            isBackStarted = true
            WebViewPool.captureSnapshot(url)
            progressFlow.collect {}
            if (canGoBack) webView.goBack() else navController.popBackStack()
        } finally {
            isBackStarted = false
        }
    }

    BackHandler(enabled = showTabSwitcher) { showTabSwitcher = false }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = mainContentScale
                    scaleY = mainContentScale
                    shape = RoundedCornerShape(mainContentCorner)
                    clip = true
                },
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .statusBarsPadding()
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            WebViewPool.captureSnapshot(url)
                            navController.popBackStack()
                        }) {
                            Icon(Icons.Default.Home, null, modifier = Modifier.size(24.dp))
                        }
                        Text(
                            text = siteName,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { isDesktopMode = !isDesktopMode }) {
                            Icon(
                                imageVector = if (isDesktopMode) Icons.Default.Computer else Icons.Default.Smartphone,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (isDesktopMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { webView.reload() }) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(24.dp))
                        }
                        IconButton(onClick = {
                            WebViewPool.captureSnapshot(url)
                            cachedTabCount = WebViewPool.getCachedUrls().size.coerceAtLeast(1)
                            showTabSwitcher = true
                        }) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(18.dp)
                                    .border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(4.dp))
                            ) {
                                Text(
                                    text = cachedTabCount.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    style = TextStyle(
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                        lineHeightStyle = LineHeightStyle(
                                            alignment = LineHeightStyle.Alignment.Center,
                                            trim = LineHeightStyle.Trim.Both
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            AndroidView(
                factory = { _ ->
                    if (webView.parent != null) {
                        (webView.parent as ViewGroup).removeView(webView)
                    }
                    webView.webViewClient = object : WebViewClient() {
                        @SuppressLint("WebViewClientOnReceivedSslError")
                        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                            handler?.proceed()
                        }
                        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                            canGoBack = view.canGoBack()
                            super.doUpdateVisitedHistory(view, url, isReload)
                        }
                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            if (desktopModeState.value) view.evaluateJavascript(desktopJs, null)
                        }
                        override fun onPageFinished(view: WebView, url: String?) {
                            super.onPageFinished(view, url)
                            if (desktopModeState.value) view.evaluateJavascript(desktopJs, null)
                        }
                    }
                    webView.webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            if (desktopModeState.value) view.evaluateJavascript(desktopJs, null)
                            if (newProgress == 100) WebViewPool.captureSnapshot(url)
                        }
                    }
                    webView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    webView
                },
                update = { view ->
                    val targetUa = if (isDesktopMode) {
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                    } else {
                        defaultUserAgent
                    }

                    if (view.settings.userAgentString != targetUa) {
                        view.settings.userAgentString = targetUa
                        view.settings.loadWithOverviewMode = isDesktopMode
                        view.settings.useWideViewPort = isDesktopMode
                        view.settings.setSupportZoom(isDesktopMode)
                        view.settings.builtInZoomControls = isDesktopMode
                        view.settings.displayZoomControls = false
                        view.reload()
                    }

                    view.onResume()
                    view.requestLayout()
                },
                onRelease = { view -> WebViewPool.onRelease(url, view) },
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )
        }

        AnimatedVisibility(
            visible = showTabSwitcher,
            enter = scaleIn(initialScale = 1.15f, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(250)),
            exit = scaleOut(targetScale = 1.15f, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(250))
        ) {
            TabSwitcherOverlay(
                currentUrl = url,
                viewModel = viewModel,
                navController = navController,
                onClose = { showTabSwitcher = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSwitcherOverlay(
    currentUrl: String,
    viewModel: WebViewModel,
    navController: NavController,
    onClose: () -> Unit
) {
    var activeUrls by remember { mutableStateOf(WebViewPool.getCachedUrls()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("标签页", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
                },
                actions = {
                    IconButton(onClick = {
                        WebViewPool.captureSnapshot(currentUrl)
                        onClose()
                        navController.popBackStack("home", false)
                    }) {
                        Icon(Icons.Default.Add, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(activeUrls, key = { it }) { tabUrl ->
                val tabName = viewModel.webSites.find { it.url == tabUrl }?.name ?: "网页"
                val isCurrent = tabUrl == currentUrl
                val snapshot = WebViewPool.getSnapshot(tabUrl)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(if (isCurrent) 8.dp else 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.75f)
                        .clickable {
                            onClose()
                            if (!isCurrent) {
                                WebViewPool.captureSnapshot(currentUrl)
                                navController.navigate("web/${URLEncoder.encode(tabUrl, "UTF-8")}") {
                                    popUpTo("home")
                                }
                            }
                        }
                        .border(
                            width = if (isCurrent) 2.5.dp else 0.dp,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Public, null, modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tabName, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    WebViewPool.remove(tabUrl)
                                    activeUrls = WebViewPool.getCachedUrls()
                                    if (activeUrls.isEmpty() || tabUrl == currentUrl) {
                                        onClose()
                                        navController.popBackStack("home", false)
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                            }
                        }
                        Box(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (snapshot != null) {
                                Image(
                                    bitmap = snapshot.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.TopCenter
                                )
                            } else {
                                Text(
                                    text = tabUrl.removePrefix("https://").removePrefix("http://"),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

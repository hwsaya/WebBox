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
import androidx.compose.animation.core.MutableTransitionState
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import java.net.URLEncoder

private val desktopModeCache = mutableMapOf<String, Boolean>()

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
    var showMenu by remember { mutableStateOf(false) }
    var isTranslated by remember(url) { mutableStateOf(false) }

    val defaultUserAgent = remember { android.webkit.WebSettings.getDefaultUserAgent(context) }
    
    var isDesktopMode by remember(url) { mutableStateOf(desktopModeCache[url] ?: false) }
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

                        // 标签页按钮换到了菜单左边
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

                        // 菜单框在最右侧
                        Box(contentAlignment = Alignment.TopEnd) {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(24.dp))
                            }
                            WebDropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                MiuixMenuItem("刷新网页") {
                                    showMenu = false
                                    webView.reload()
                                }
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                                MiuixMenuItem(if (isDesktopMode) "切换手机模式" else "切换电脑模式") {
                                    showMenu = false
                                    isDesktopMode = !isDesktopMode
                                    desktopModeCache[url] = isDesktopMode
                                }
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                                MiuixMenuItem(if (isTranslated) "取消翻译" else "翻译网页") {
                                    showMenu = false
                                    if (isTranslated) {
                                        webView.reload()
                                    } else {
                                        isTranslated = true
                                        val js = """
                                            (function() {
                                                if (document.getElementById('google_translate_element')) return;
                                                
                                                var style = document.createElement('style');
                                                style.type = 'text/css';
                                                style.innerHTML = 'body { top: 0 !important; } .skiptranslate { display: none !important; } #goog-gt-tt { display: none !important; }';
                                                document.head.appendChild(style);

                                                var div = document.createElement('div');
                                                div.id = 'google_translate_element';
                                                div.style.display = 'none';
                                                document.body.appendChild(div);

                                                var script = document.createElement('script');
                                                script.type = 'text/javascript';
                                                script.src = 'https://translate.google.com/translate_a/element.js?cb=googleTranslateElementInit';
                                                document.head.appendChild(script);

                                                window.googleTranslateElementInit = function() {
                                                    new google.translate.TranslateElement({
                                                        pageLanguage: 'auto',
                                                        includedLanguages: 'zh-CN',
                                                        layout: google.translate.TranslateElement.InlineLayout.SIMPLE,
                                                        autoDisplay: false
                                                    }, 'google_translate_element');
                                                    
                                                    setTimeout(function() {
                                                        var select = document.querySelector('select.goog-te-combo');
                                                        if (select) {
                                                            select.value = 'zh-CN';
                                                            select.dispatchEvent(new Event('change'));
                                                        }
                                                    }, 1200);
                                                };
                                            })();
                                        """.trimIndent()
                                        webView.evaluateJavascript(js, null)
                                    }
                                }
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
                            isTranslated = false // 页面开始加载时重置翻译状态
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

// 专门为 WebScreen 定制的菜单，解决了物理像素偏移遮挡图标的问题
@Composable
fun WebDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    val yOffset = with(density) { 48.dp.roundToPx() } // 自适应各分辨率设备的 48dp 下偏移

    val transitionState = remember { MutableTransitionState(false) }.apply { targetState = expanded }
    if (transitionState.currentState || transitionState.targetState) {
        Popup(
            alignment = Alignment.TopEnd,
            offset = IntOffset(0, yOffset),
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true)
        ) {
            AnimatedVisibility(
                visibleState = transitionState,
                enter = scaleIn(BouncyFloat, transformOrigin = TransformOrigin(1f, 0f)) + fadeIn(tween(150)),
                exit = scaleOut(tween(200, easing = FastOutSlowInEasing), transformOrigin = TransformOrigin(1f, 0f)) + fadeOut(tween(150))
            ) {
                Card(
                    modifier = Modifier.padding(8.dp).width(IntrinsicSize.Max).shadow(12.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.defaultMinSize(minWidth = 150.dp).padding(vertical = 8.dp),
                        content = content
                    )
                }
            }
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

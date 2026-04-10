package com.example.webbox

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyGridState
import org.burnoutcrew.reorderable.reorderable
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

val BouncyFloat    = spring<Float>(dampingRatio = 0.6f, stiffness = 250f)
val BouncyIntOffset = spring<IntOffset>(dampingRatio = 0.6f, stiffness = 250f)
val BouncyIntSize  = spring<IntSize>(dampingRatio = 0.6f, stiffness = 250f)
val BouncyDp       = spring<Dp>(dampingRatio = 0.6f, stiffness = 250f)

@Composable
fun AppTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            background = Color(0xFF000000), surfaceVariant = Color(0xFF1C1C1E),
            onSurfaceVariant = Color(0xFFFFFFFF), primary = Color(0xFFFFFFFF)
        )
    } else {
        lightColorScheme(
            background = Color(0xFFF2F2F7), surfaceVariant = Color(0xFFFFFFFF),
            onSurfaceVariant = Color(0xFF000000), primary = Color(0xFF000000)
        )
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val nightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        if (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            setTheme(android.R.style.Theme_DeviceDefault_NoActionBar)
        } else {
            setTheme(android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        }
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = isSystemInDarkTheme()
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                                     else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                    navigationBarStyle = if (darkTheme) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                                         else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                onDispose {}
            }
            AppTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: WebViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(300)) },
        popEnterTransition = { fadeIn(tween(300)) },
        popExitTransition = { fadeOut(tween(300)) }
    ) {
        composable("home") { HomeScreen(navController, viewModel) }
        composable("web/{url}") { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
            WebScreen(url = url, navController = navController, viewModel = viewModel)
        }
        composable("auto_login_settings") { AutoLoginScreen(navController, viewModel) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: WebViewModel) {
    var showAddSheet by remember { mutableStateOf(false) }
    var showWebDavSheet by remember { mutableStateOf(false) }
    var showLruSheet by remember { mutableStateOf(false) }
    var siteToEdit by remember { mutableStateOf<WebSite?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.targetPage }.collect { page ->
            if (page == 1) { isEditMode = false; menuExpanded = false }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    tonalElevation = 0.dp
                ) {
                    val navColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                    val targetPage = pagerState.targetPage

                    NavigationBarItem(
                        selected = targetPage == 0,
                        onClick = {
                            if (targetPage != 0) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0, animationSpec = tween(300, easing = FastOutSlowInEasing))
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "主页", modifier = Modifier.size(24.dp)) },
                        label = { Text("主页", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = navColors
                    )
                    NavigationBarItem(
                        selected = targetPage == 1,
                        onClick = {
                            if (targetPage != 1) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1, animationSpec = tween(300, easing = FastOutSlowInEasing))
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "设置", modifier = Modifier.size(24.dp)) },
                        label = { Text("设置", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = navColors
                    )
                }
            }
        ) { paddingValues ->
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) { page ->
                if (page == 0) {
                    SiteGrid(
                        viewModel = viewModel,
                        navController = navController,
                        isEditMode = isEditMode,
                        onEditModeToggle = { isEditMode = !isEditMode },
                        onAddClick = { showAddSheet = true },
                        onEditClick = { siteToEdit = it },
                        menuExpanded = menuExpanded,
                        onMenuExpandedChange = { menuExpanded = it }
                    )
                } else {
                    SettingsTab(
                        navController = navController,
                        onWebDavClick = { showWebDavSheet = true },
                        onLruClick = { showLruSheet = true }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = menuExpanded,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { menuExpanded = false }
            )
        }
    }

    if (showLruSheet) LruBottomSheet(viewModel) { showLruSheet = false }
    if (showWebDavSheet) WebDavBottomSheet(viewModel) { showWebDavSheet = false }
    if (showAddSheet) SiteBottomSheet(null, viewModel) { showAddSheet = false }
    siteToEdit?.let { SiteBottomSheet(it, viewModel) { siteToEdit = null } }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SiteGrid(
    viewModel: WebViewModel,
    navController: NavController,
    isEditMode: Boolean,
    onEditModeToggle: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (WebSite) -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val reorderState = rememberReorderableLazyGridState(onMove = { from, to ->
        viewModel.swapSites(from.index, to.index)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    })

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "WebBox",
                fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp, color = MaterialTheme.colorScheme.primary
            )
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onMenuExpandedChange(true) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Layers, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                }
                MiuixMenu(menuExpanded, { onMenuExpandedChange(false) }) {
                    MiuixMenuItem("添加新网页") {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onMenuExpandedChange(false)
                        onAddClick()
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )
                    MiuixMenuItem(if (isEditMode) "退出编辑" else "进入编辑") {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onMenuExpandedChange(false)
                        onEditModeToggle()
                    }
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = reorderState.gridState,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().reorderable(reorderState)
        ) {
            items(viewModel.webSites, key = { it.url }) { site ->
                ReorderableItem(reorderState, site.url) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, BouncyDp, label = "")
                    val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, BouncyFloat, label = "")
                    val borderAlpha by animateFloatAsState(if (isDragging) 0f else 0.05f, tween(200), label = "")

                    LaunchedEffect(isDragging) {
                        if (isDragging) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(elevation),
                        modifier = Modifier
                            .animateItem(placementSpec = BouncyIntOffset)
                            .fillMaxWidth()
                            .height(76.dp)
                            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = borderAlpha), RoundedCornerShape(16.dp))
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clip(RoundedCornerShape(16.dp))
                            .detectReorderAfterLongPress(reorderState)
                            .clickable {
                                if (!isDragging) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (isEditMode) onEditClick(site)
                                    else navController.navigate("web/${URLEncoder.encode(site.url, "UTF-8")}")
                                }
                            }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (isEditMode) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(10.dp)
                                        .size(18.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(0.1f), CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                            Text(
                                site.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.align(Alignment.Center).padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiuixMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val transitionState = remember { MutableTransitionState(false) }.apply { targetState = expanded }
    if (transitionState.currentState || transitionState.targetState) {
        Popup(
            alignment = Alignment.TopEnd,
            offset = IntOffset(0, 60),
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

@Composable
fun MiuixMenuItem(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BouncySheetContent(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    var isAppeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isAppeared = true }
    val offsetY by animateDpAsState(if (isAppeared) 0.dp else 200.dp, BouncyDp, label = "")
    Column(modifier = modifier.offset(y = offsetY), content = content)
}

@Composable
fun SettingsTab(navController: NavController, onWebDavClick: () -> Unit, onLruClick: () -> Unit) {
    val context = LocalContext.current
    val showToast = { Toast.makeText(context, "规划中", Toast.LENGTH_SHORT).show() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "设置", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 12.dp)
        )
        SettingsCard("LRU 独立实例", "设置网页后台驻留数量", Icons.Default.Build, onLruClick)
        SettingsCard("WebDAV 同步", "备份站点与登录态数据", Icons.Default.Refresh, onWebDavClick)
        SettingsCard("自动登录配置", "管理已添加网页的自动登录账号与密码", Icons.Default.Lock) { navController.navigate("auto_login_settings") }
        SettingsCard("主题风格", "自定义应用外观与配色", Icons.Default.Star, showToast)
        SettingsCard("Web 分组", "为您的站点添加分类标签", Icons.Default.List, showToast)
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SettingsCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f), fontSize = 13.sp)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LruBottomSheet(viewModel: WebViewModel, onDismiss: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var showInfoDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        BouncySheetContent(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("LRU 独立实例", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showInfoDialog = true },
                    modifier = Modifier.size(20.dp).padding(start = 4.dp)
                ) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (viewModel.lruEnabled) "已开启驻留" else "退出即销毁", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Switch(
                    checked = viewModel.lruEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.updateLruSettings(it, viewModel.lruSize)
                    }
                )
            }
            AnimatedVisibility(visible = viewModel.lruEnabled) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("最大实例数", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.8f))
                        Text("${viewModel.lruSize} 个", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = viewModel.lruSize.toFloat(),
                        onValueChange = { v ->
                            val newValue = v.toInt()
                            if (newValue != viewModel.lruSize) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateLruSettings(viewModel.lruEnabled, newValue)
                            }
                        },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            activeTickColor = Color.Transparent,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                            inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            title = { Text("功能说明", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "开启后，应用将在后台保留最近访问的网页实例，切换网页时可实现秒开并保留浏览状态。" +
                    "关闭后，每次退出即销毁实例，节省内存与电量。",
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("明白", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavBottomSheet(viewModel: WebViewModel, onDismiss: () -> Unit) {
    var davUrl by remember { mutableStateOf("") }
    var davUser by remember { mutableStateOf("") }
    var davPass by remember { mutableStateOf("") }
    var davAuto by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        val config = viewModel.loadWebDavConfig()
        davUrl = config.url; davUser = config.user; davPass = config.pass; davAuto = config.autoBackup
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun saveConfig() = viewModel.saveWebDavConfig(WebDavConfig(davUrl, davUser, davPass, davAuto))

    ModalBottomSheet(
        onDismissRequest = { saveConfig(); onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp)
    ) {
        BouncySheetContent(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "WebDAV 同步配置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            OutlinedTextField(davUrl, { davUrl = it }, label = { Text("地址") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
            OutlinedTextField(davUser, { davUser = it }, label = { Text("账号") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
            OutlinedTextField(davPass, { davPass = it }, label = { Text("密码") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("自动同步", fontSize = 16.sp)
                Switch(checked = davAuto, onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    davAuto = it
                    saveConfig()
                })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        saveConfig()
                        viewModel.backupToWebDav()
                    },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, null)
                    Text("备份")
                }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        saveConfig()
                        viewModel.restoreFromWebDav()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.primary.copy(0.2f)
                    )
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, null)
                    Text("恢复")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteBottomSheet(site: WebSite?, viewModel: WebViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(site?.name ?: "") }
    var url by remember { mutableStateOf(site?.url ?: "") }
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp)
    ) {
        BouncySheetContent(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                if (site != null) "编辑网页" else "添加新网页",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            OutlinedTextField(name, { name = it }, label = { Text("名称") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
            OutlinedTextField(url, { url = it }, label = { Text("URL") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (name.isNotBlank() && url.isNotBlank()) {
                            if (site != null) viewModel.updateSite(site, name, url)
                            else viewModel.addSite(name, url)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) { Text("保存") }

                if (site != null) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.deleteSite(site)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.error.copy(0.5f)
                        )
                    ) { Text("删除") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoLoginScreen(navController: NavController, viewModel: WebViewModel) {
    var selectedSite by remember { mutableStateOf<WebSite?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自动登录配置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(contentPadding = paddingValues, modifier = Modifier.fillMaxSize()) {
            items(viewModel.webSites) { site ->
                val cred = viewModel.getCredentialForUrl(site.url)
                val subtitle = if (cred != null && cred.user.isNotBlank()) "已配置: ${cred.user}" else "未配置"
                
                SettingsCard(
                    title = site.name,
                    subtitle = subtitle,
                    icon = Icons.Default.Public,
                    onClick = { selectedSite = site }
                )
            }
        }
    }

    selectedSite?.let { site ->
        var user by remember { mutableStateOf(viewModel.getCredentialForUrl(site.url)?.user ?: "") }
        var pass by remember { mutableStateOf(viewModel.getCredentialForUrl(site.url)?.pass ?: "") }
        
        AlertDialog(
            onDismissRequest = { selectedSite = null },
            title = { Text("配置 ${site.name}") },
            text = {
                Column {
                    OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("账号/邮箱") })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pass, 
                        onValueChange = { pass = it }, 
                        label = { Text("密码") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { 
                    viewModel.saveCredential(site.url, user, pass)
                    selectedSite = null 
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { selectedSite = null }) { Text("取消") }
            }
        )
    }
}
package com.creditdb.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.creditdb.pro.data.AppSettings
import com.creditdb.pro.data.CreditRepository
import com.creditdb.pro.ui.components.DatabaseUpdateDialog
import com.creditdb.pro.ui.components.ThemePaletteBottomSheet
import com.creditdb.pro.ui.glass.BottomFadeScrim
import com.creditdb.pro.ui.glass.BottomTab
import com.creditdb.pro.ui.glass.FLOATING_BAR_MAX_WIDTH
import com.creditdb.pro.ui.glass.FloatingBarsTapGuard
import com.creditdb.pro.ui.glass.FloatingTopBarRow
import com.creditdb.pro.ui.glass.GlassDropdownMenu
import com.creditdb.pro.ui.glass.GlassMenuController
import com.creditdb.pro.ui.glass.GlassMenuHost
import com.creditdb.pro.ui.glass.GlassSettings
import com.creditdb.pro.ui.glass.GlassSheetController
import com.creditdb.pro.ui.glass.GlassSheetHost
import com.creditdb.pro.ui.glass.GlassTabBar
import com.creditdb.pro.ui.glass.LocalAppBackdrop
import com.creditdb.pro.ui.glass.LocalChromePadding
import com.creditdb.pro.ui.glass.LocalFloatingBarsHeightPx
import com.creditdb.pro.ui.glass.LocalGlassMenu
import com.creditdb.pro.ui.glass.LocalGlassSheets
import com.creditdb.pro.ui.glass.LocalLiquidGlassEnabled
import com.creditdb.pro.ui.glass.LocalOverflowMenuButton
import com.creditdb.pro.ui.glass.LocalTopBarController
import com.creditdb.pro.ui.glass.TopBarController
import com.creditdb.pro.ui.glass.TopBarGlassButton
import com.creditdb.pro.ui.glass.TopBarTitleCapsule
import com.creditdb.pro.ui.glass.TopFadeBlur
import com.creditdb.pro.ui.glass.backdrop.backdrops.layerBackdrop
import com.creditdb.pro.ui.glass.backdrop.backdrops.rememberLayerBackdrop
import com.creditdb.pro.ui.glass.floatingtabbar.rememberFloatingTabBarScrollConnection
import com.creditdb.pro.ui.glass.isGlassSupported
import com.creditdb.pro.ui.glass.topBarContentPadding
import com.creditdb.pro.ui.guide.GuideScreen
import com.creditdb.pro.ui.navigation.Screen
import com.creditdb.pro.ui.predict.PredictScreen
import com.creditdb.pro.ui.staff.StaffDetailScreen
import com.creditdb.pro.ui.staff.StaffScreen
import com.creditdb.pro.ui.staff.StudioDetailScreen
import com.creditdb.pro.ui.theme.CreditDBProTheme
import com.creditdb.pro.ui.tier.TierScreen
import com.creditdb.pro.ui.works.WorkDetailScreen
import com.creditdb.pro.ui.works.WorksScreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import java.net.URLDecoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
        GlassSettings.init(this)
        com.creditdb.pro.ui.theme.LanguageManager.init(this)
        com.creditdb.pro.data.StaffNameResolver.init(this)
        com.creditdb.pro.data.CharacterNameResolver.init(this)

        // Coil ImageLoader 設定 (Bangumi用User-Agent設定 & メモリ・ディスクキャッシュ構成)
        val imageLoader = coil.ImageLoader.Builder(this)
            .okHttpClient {
                okhttp3.OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header("User-Agent", "CreditDB-Android/1.0 (Linux; Android)")
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            }
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100 MB
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
        coil.Coil.setImageLoader(imageLoader)

        enableEdgeToEdge()
        setContent {
            CreditDBProTheme {
                // DB の準備（初回は 200MB 超の展開）を UI スレッドで行うと固まるので、
                // バックグラウンドで済ませてからメイン画面を出す
                var dbReady by remember { mutableStateOf(com.creditdb.pro.data.DatabaseHelper.isInitialized()) }
                if (dbReady) {
                    MainApp()
                } else {
                    StartupScreen()
                    val appContext = LocalContext.current.applicationContext
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            com.creditdb.pro.data.DatabaseHelper.getInstance(appContext)
                            CreditRepository.getInstance(appContext)
                            com.creditdb.pro.data.PredictionEngine.getInstance(appContext)
                        }
                        dbReady = true
                    }
                }
            }
        }
    }
}

/** 起動時の DB 準備中に表示する画面 */
@Composable
fun StartupScreen() {
    val isEn = com.creditdb.pro.ui.theme.LanguageManager.isEnglish
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "CreditDB",
                style = MaterialTheme.typography.headlineMediumEmphasized,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(24.dp))
            com.creditdb.pro.ui.components.AppLoadingIndicator(contained = true, size = 72.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isEn) "Preparing database..." else "データベースを準備しています…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isEn) "The first launch may take a little while." else "初回起動時は少し時間がかかります",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val repository = remember { CreditRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    val liquidGlassEnabled by GlassSettings.liquidGlass.collectAsState()
    val reduceDynamicBlur by GlassSettings.reduceDynamicBlur.collectAsState()

    val paintBackdrop = MaterialTheme.colorScheme.background
    val appBackdrop = rememberLayerBackdrop {
        drawRect(paintBackdrop)
        drawContent()
    }
    val hazeState = remember { HazeState() }
    val glassSheets = remember { GlassSheetController() }
    val glassMenu = remember { GlassMenuController() }
    val topBarController = remember { TopBarController() }
    val tabBarScrollConnection = rememberFloatingTabBarScrollConnection()
    val floatingBarsHeight = remember { mutableIntStateOf(0) }

    // タブ遷移時は常に下部タブバーを展開状態に戻す
    LaunchedEffect(currentRoute) {
        tabBarScrollConnection.expand()
    }

    // 手動データベース更新モーダル状態
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }
    var updatePercent by remember { mutableIntStateOf(0) }
    var updateStatusText by remember { mutableStateOf("待機中...") }

    // カラーテーマプリセット選択シート状態
    var showThemeSheet by remember { mutableStateOf(false) }

    fun triggerDatabaseUpdate() {
        showUpdateDialog = true
        isUpdating = true
        updatePercent = 0
        updateStatusText = "データベース更新開始..."
        coroutineScope.launch {
            repository.simulateDatabaseUpdate { p, text ->
                updatePercent = p
                updateStatusText = text
                if (p >= 100) {
                    isUpdating = false
                }
            }
        }
    }

    val isTopLevelRoute = currentRoute in Screen.bottomNavItems.map { it.route }
    val density = LocalDensity.current
    val topChrome = topBarContentPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val measuredTabBarDp = with(density) { floatingBarsHeight.intValue.toDp() }
    val bottomChrome = if (isTopLevelRoute) {
        if (measuredTabBarDp > 0.dp) measuredTabBarDp + 12.dp else navBarBottom + 96.dp
    } else {
        navBarBottom + 16.dp
    }
    val chromePadding = PaddingValues(top = topChrome, bottom = bottomChrome)

    val overflowMenuButton: @Composable () -> Unit = {
        val isEnLang = com.creditdb.pro.ui.theme.LanguageManager.isEnglish
        var menuOpen by remember { mutableStateOf(false) }
        Box {
            TopBarGlassButton(
                onClick = { menuOpen = true },
                icon = Icons.Default.MoreVert,
                contentDescription = if (isEnLang) "More" else "メニュー",
            )
            GlassDropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                // ヘッダー: CreditDB PRO
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "CreditDB",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        Text(
                            text = "PRO",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                DropdownMenuItem(
                    text = {
                        Text(if (isEnLang) "Language: EN → JP" else "表示言語: JP → EN")
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Translate, contentDescription = null)
                    },
                    onClick = {
                        menuOpen = false
                        com.creditdb.pro.ui.theme.LanguageManager.toggleLanguage()
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(if (isEnLang) "Update Database" else "データベース手動更新")
                    },
                    leadingIcon = {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                    },
                    onClick = {
                        menuOpen = false
                        triggerDatabaseUpdate()
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(if (isEnLang) "Mathematical Guide" else "数理解説ガイド")
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null)
                    },
                    onClick = {
                        menuOpen = false
                        navController.navigate(Screen.Guide.route)
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(if (isEnLang) "Theme & Display" else "テーマ・表示設定")
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Palette, contentDescription = null)
                    },
                    onClick = {
                        menuOpen = false
                        showThemeSheet = true
                    },
                )
            }
        }
    }

    val glassBackdropActive = (liquidGlassEnabled && isGlassSupported()) || !reduceDynamicBlur

    CompositionLocalProvider(
        LocalLiquidGlassEnabled provides liquidGlassEnabled,
        LocalAppBackdrop provides appBackdrop,
        LocalGlassSheets provides glassSheets,
        LocalGlassMenu provides glassMenu,
        LocalTopBarController provides topBarController,
        LocalOverflowMenuButton provides overflowMenuButton,
        LocalChromePadding provides chromePadding,
        LocalFloatingBarsHeightPx provides if (isTopLevelRoute) floatingBarsHeight.intValue else 0,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. 最背面: ページ本体 (NavHost) — 画面端から端まで描画し、appBackdrop と hazeState に記録
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (glassBackdropActive) Modifier.layerBackdrop(appBackdrop) else Modifier)
                        .hazeSource(hazeState)
                        .nestedScroll(tabBarScrollConnection),
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Works.route,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        // タブ 1: 作品一覧
                        composable(Screen.Works.route) {
                            WorksScreen(
                                onWorkClick = { workId ->
                                    navController.navigate(Screen.WorkDetail.createRoute(workId))
                                }
                            )
                        }

                        // タブ 2: クオリティ予測 & チーム編成
                        composable(Screen.Predict.route) {
                            PredictScreen()
                        }

                        // タブ 3: 制作陣・声優リーダーボード
                        composable(Screen.Staff.route) {
                            StaffScreen(
                                onStaffClick = { staffName ->
                                    navController.navigate(Screen.StaffDetail.createRoute(staffName))
                                },
                                onStudioClick = { studioName ->
                                    navController.navigate(Screen.StudioDetail.createRoute(studioName))
                                }
                            )
                        }

                        // タブ 4: Tier表
                        composable(Screen.Tier.route) {
                            TierScreen(
                                onNavigateToWork = { workId ->
                                    navController.navigate(Screen.WorkDetail.createRoute(workId))
                                },
                                onNavigateToStaff = { staffName ->
                                    navController.navigate(Screen.StaffDetail.createRoute(staffName))
                                },
                                onNavigateToStudio = { studioName ->
                                    navController.navigate(Screen.StudioDetail.createRoute(studioName))
                                }
                            )
                        }

                        // 数理解説ガイド
                        composable(Screen.Guide.route) {
                            GuideScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        // サブ画面: 作品詳細
                        composable(
                            route = Screen.WorkDetail.route,
                            arguments = listOf(navArgument("workId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val workId = backStackEntry.arguments?.getString("workId") ?: ""
                            WorkDetailScreen(
                                workId = workId,
                                onBackClick = { navController.popBackStack() },
                                onStaffClick = { staffName ->
                                    navController.navigate(Screen.StaffDetail.createRoute(staffName))
                                },
                                onWorkClick = { wid ->
                                    navController.navigate(Screen.WorkDetail.createRoute(wid))
                                },
                                onStudioClick = { studioName ->
                                    navController.navigate(Screen.StudioDetail.createRoute(studioName))
                                }
                            )
                        }

                        // サブ画面: スタッフ詳細
                        composable(
                            route = Screen.StaffDetail.route,
                            arguments = listOf(navArgument("staffName") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val rawStaffName = backStackEntry.arguments?.getString("staffName") ?: ""
                            val staffName = try {
                                URLDecoder.decode(rawStaffName, "UTF-8")
                            } catch (e: Exception) {
                                rawStaffName
                            }

                            StaffDetailScreen(
                                staffName = staffName,
                                onBackClick = { navController.popBackStack() },
                                onWorkClick = { workId ->
                                    navController.navigate(Screen.WorkDetail.createRoute(workId))
                                }
                            )
                        }

                        // サブ画面: 制作スタジオ詳細
                        composable(
                            route = Screen.StudioDetail.route,
                            arguments = listOf(navArgument("studioName") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val rawStudioName = backStackEntry.arguments?.getString("studioName") ?: ""
                            val studioName = try {
                                URLDecoder.decode(rawStudioName, "UTF-8")
                            } catch (e: Exception) {
                                rawStudioName
                            }

                            StudioDetailScreen(
                                studioName = studioName,
                                onBackClick = { navController.popBackStack() },
                                onWorkClick = { workId ->
                                    navController.navigate(Screen.WorkDetail.createRoute(workId))
                                },
                                onStaffClick = { staffName ->
                                    navController.navigate(Screen.StaffDetail.createRoute(staffName))
                                }
                            )
                        }
                    }
                }

                // 2. 上部フェードブラー
                TopFadeBlur(
                    hazeState = hazeState,
                    pageColor = MaterialTheme.colorScheme.background,
                    scrimColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier.align(Alignment.TopCenter),
                )

                // 3. 下部フェードスクリム & タップガード（トップレベル4タブ表示時のみ）
                if (isTopLevelRoute) {
                    BottomFadeScrim(
                        pageColor = MaterialTheme.colorScheme.background,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                    FloatingBarsTapGuard(
                        barsHeight = { floatingBarsHeight.intValue },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }

                // 4. 上部1行フローティングコントロール
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                ) {
                    val customTopBar = topBarController.content
                    if (customTopBar != null) {
                        customTopBar()
                    } else if (isTopLevelRoute) {
                        FloatingTopBarRow {
                            TopBarTitleCapsule(title = "CreditDB PRO")
                            Spacer(modifier = Modifier.weight(1f))
                            LocalOverflowMenuButton.current()
                        }
                    }
                }

                // 5. 下部フローティング GlassTabBar（4タブ）
                androidx.compose.animation.AnimatedVisibility(
                    visible = isTopLevelRoute,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .widthIn(max = FLOATING_BAR_MAX_WIDTH)
                        .fillMaxWidth()
                        .onSizeChanged { floatingBarsHeight.intValue = it.height },
                ) {
                    val isEn = com.creditdb.pro.ui.theme.LanguageManager.isEnglish
                    val selectedIndex = Screen.bottomNavItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
                    val bottomTabs = remember(isEn, selectedIndex) {
                        Screen.bottomNavItems.mapIndexed { idx, screen ->
                            val isSelected = idx == selectedIndex
                            BottomTab(
                                label = screen.getLocalizedTitle(isEn),
                                icon = (if (isSelected) screen.selectedIcon else screen.unselectedIcon) ?: Icons.Default.MoreVert,
                            )
                        }
                    }
                    GlassTabBar(
                        tabs = bottomTabs,
                        selectedIndex = selectedIndex,
                        onTabSelected = { idx ->
                            val targetRoute = Screen.bottomNavItems.getOrNull(idx)?.route ?: return@GlassTabBar
                            if (currentRoute != targetRoute) {
                                navController.navigate(targetRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        scrollConnection = tabBarScrollConnection,
                        useGlass = liquidGlassEnabled && isGlassSupported(),
                    )
                }

                // 6. インプロセス GlassSheetHost / GlassMenuHost
                GlassSheetHost(controller = glassSheets, glassBackdrop = glassBackdropActive)
                GlassMenuHost(controller = glassMenu, glassBackdrop = glassBackdropActive)
            }
        }

        // 手動データベース更新モーダル（CompositionLocalProvider 内側に配置して GlassAlertDialog が appBackdrop を参照できるようにする）
        if (showUpdateDialog) {
            DatabaseUpdateDialog(
                isUpdating = isUpdating,
                percent = updatePercent,
                statusText = updateStatusText,
                onDismiss = { showUpdateDialog = false }
            )
        }

        // カラーテーマプリセット選択シート
        if (showThemeSheet) {
            ThemePaletteBottomSheet(
                onDismissRequest = { showThemeSheet = false }
            )
        }
    }
}

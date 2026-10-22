package com.creditdb.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.creditdb.pro.data.AppSettings
import com.creditdb.pro.data.CreditRepository
import com.creditdb.pro.ui.components.DatabaseUpdateDialog
import com.creditdb.pro.ui.components.ThemePaletteBottomSheet
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
import kotlinx.coroutines.launch
import java.net.URLDecoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
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

    // パフォーマンスを最優先し、再レイアウト負荷のない固定TopAppBar
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (isTopLevelRoute) {
                TopAppBar(
                    title = {
                        // ロゴは文字サイズ設定に追従させず固定サイズで描く（大きな文字設定・狭い画面での崩れ防止）
                        val density = androidx.compose.ui.platform.LocalDensity.current
                        val logoSize = with(density) { 22.dp.toSp() }
                        val proSize = with(density) { 10.dp.toSp() }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "CreditDB",
                                style = MaterialTheme.typography.titleLargeEmphasized.copy(fontSize = logoSize, lineHeight = logoSize),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ) {
                                Text(
                                    text = "PRO",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = proSize, lineHeight = proSize),
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    },
                    actions = {
                        val isEnLang = com.creditdb.pro.ui.theme.LanguageManager.isEnglish
                        // 言語切り替え: 塗り＋その上の文字色の組で、どのテーマでも読みやすくする
                        Surface(
                            onClick = { com.creditdb.pro.ui.theme.LanguageManager.toggleLanguage() },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = if (isEnLang) "EN" else "JP",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLargeEmphasized,
                                maxLines = 1
                            )
                        }

                        // 手動データベース更新ボタン
                        IconButton(onClick = { triggerDatabaseUpdate() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = if (isEnLang) "Update DB" else "データベース手動更新",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // ガイド・テーマはメニューにまとめてタイトルの幅を確保する
                        var menuOpen by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = if (isEnLang) "More" else "その他",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                                shape = MaterialTheme.shapes.large
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isEnLang) "Guide" else "数理解説ガイド") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        navController.navigate(Screen.Guide.route)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isEnLang) "Theme & Language" else "テーマ・言語設定") },
                                    leadingIcon = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        showThemeSheet = true
                                    }
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isTopLevelRoute,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                ShortNavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    val isEn = com.creditdb.pro.ui.theme.LanguageManager.isEnglish
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        val labelText = screen.getLocalizedTitle(isEn)
                        ShortNavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                val icon = if (selected) screen.selectedIcon else screen.unselectedIcon
                                if (icon != null) {
                                    Icon(imageVector = icon, contentDescription = labelText)
                                }
                            },
                            label = {
                                Text(
                                    text = labelText,
                                    style = if (selected) MaterialTheme.typography.labelMediumEmphasized else MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            },
                            colors = ShortNavigationBarItemDefaults.colors(
                                selectedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColorTopIconPosition = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val topPadding = if (isTopLevelRoute) innerPadding.calculateTopPadding() else 0.dp
        val bottomPadding = if (isTopLevelRoute) innerPadding.calculateBottomPadding() else 0.dp

        NavHost(
            navController = navController,
            startDestination = Screen.Works.route,
            modifier = Modifier.padding(top = topPadding, bottom = bottomPadding)
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

    // 手動データベース更新モーダル
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

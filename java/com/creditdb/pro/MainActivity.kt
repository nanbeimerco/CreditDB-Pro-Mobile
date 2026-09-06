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
import com.creditdb.pro.ui.works.WorkDetailScreen
import com.creditdb.pro.ui.works.WorksScreen
import kotlinx.coroutines.launch
import java.net.URLDecoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
        enableEdgeToEdge()
        setContent {
            CreditDBProTheme {
                MainApp()
            }
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
    val repository = remember { CreditRepository(context) }
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "CreditDB",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "PRO",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    },
                    actions = {
                        // 数理解説ガイド画面への遷移ボタン
                        IconButton(
                            onClick = { navController.navigate(Screen.Guide.route) }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                contentDescription = "数理解説ガイド",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // カラーテーマ・プリセット変更ボタン
                        IconButton(
                            onClick = { showThemeSheet = true }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = "カラーテーマ変更",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 手動データベース更新ボタン
                        IconButton(
                            onClick = { triggerDatabaseUpdate() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "データベース手動更新",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
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
                                    Icon(imageVector = icon, contentDescription = screen.title)
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary
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

            // タブ 4: 制作陣・声優リーダーボード
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

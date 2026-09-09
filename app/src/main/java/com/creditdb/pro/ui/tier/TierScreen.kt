package com.creditdb.pro.ui.tier

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.creditdb.pro.ui.theme.AppStrings
import com.creditdb.pro.ui.theme.LanguageManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TierScreen(
    onNavigateToWork: (workId: String) -> Unit,
    onNavigateToStaff: (staffName: String) -> Unit,
    onNavigateToStudio: (studioName: String) -> Unit,
    viewModel: TierViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEn = LanguageManager.isEnglish
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    val totalRankedAnime = remember(uiState.config) {
        uiState.config.rows.sumOf { it.items.size }
    }
    val correlation = remember(uiState.config) {
        com.creditdb.pro.data.tier.TierAnalysisEngine.calculateTasteCorrelation(uiState.config)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. トップバー (Title + Subtitle + 4つの機能アイコン: 分析, 診断, 行編集, エクスポート)
            val rhoStr = if (correlation.sampleSize >= 3) {
                String.format(java.util.Locale.US, "%+.2f", correlation.spearmanRho)
            } else {
                "--"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isEn) "Tier List" else "Tier表",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEn) "$totalRankedAnime works" else "$totalRankedAnime 件格付け中",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Text(
                            text = if (isEn) "ρ $rhoStr" else "相関 ρ $rhoStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.openStaffAffinitySheet() },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = if (isEn) "Staff Affinity" else "スタッフ集計",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.openCorrelationSheet() },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = if (isEn) "Taste Correlation" else "数理相関診断",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.openCustomizationDialog() },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = if (isEn) "Customize Tiers" else "Tier設定",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.openExportDialog() },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = if (isEn) "Export" else "エクスポート",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 3. Tier表 メインリスト (有限7行をColumn+verticalScrollで常時滑らかスクロール)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                uiState.config.rows.forEach { row ->
                    key(row.id) {
                        TierRowComponent(
                            row = row,
                            isExpanded = uiState.expandedRowIds.contains(row.id),
                            onToggleExpand = { viewModel.toggleRowExpand(row.id) },
                            onAnimeClick = { anime ->
                                onNavigateToWork(anime.id)
                            },
                            onAnimeLongClick = { anime, r ->
                                viewModel.openAnimeActionSheet(anime, r)
                            },
                            onAddAnimeClick = { r ->
                                viewModel.openSearchSheet(targetRowId = r.id)
                            },
                            onEditRowClick = {
                                viewModel.openCustomizationDialog()
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    // --- 各種ダイアログ & ボトムシート ---

    // 1. 作品追加検索シート
    if (uiState.showSearchSheet) {
        TierSearchBottomSheet(
            allRows = uiState.config.rows,
            searchResults = uiState.searchResults,
            isSearching = uiState.isSearching,
            onQueryChange = { q -> viewModel.onSearchQueryChange(q) },
            onAddWorkToRow = { work, rowId ->
                viewModel.addWorkToRow(work, rowId)
            },
            onDismiss = { viewModel.closeSearchSheet() }
        )
    }

    // 2. 作品長押しアクションシート
    if (uiState.selectedAnimeForAction != null && uiState.selectedRowForAction != null) {
        TierAnimeActionSheet(
            anime = uiState.selectedAnimeForAction!!,
            currentRow = uiState.selectedRowForAction!!,
            allRows = uiState.config.rows,
            onDismiss = { viewModel.closeAnimeActionSheet() },
            onMoveToRow = { targetRowId ->
                viewModel.moveAnimeToRow(uiState.selectedAnimeForAction!!.id, targetRowId)
            },
            onRemoveFromTier = {
                viewModel.removeAnimeFromTier(uiState.selectedAnimeForAction!!.id)
            },
            onViewDetails = {
                onNavigateToWork(uiState.selectedAnimeForAction!!.id)
            }
        )
    }

    // 3. Customization Dialog
    if (uiState.showCustomizationDialog) {
        TierCustomizationDialog(
            rows = uiState.config.rows,
            onDismiss = { viewModel.closeCustomizationDialog() },
            onReorderRows = { from, to -> viewModel.reorderRows(from, to) },
            onUpdateRow = { id, name, color -> viewModel.updateRow(id, name, color) },
            onAddRow = { name, color -> viewModel.addRow(name, color) },
            onDeleteRow = { id -> viewModel.deleteRow(id) },
            onHarmonizeColors = { viewModel.harmonizeColors() },
            onResetToDefault = { viewModel.resetToDefault() },
            onClearAllItems = { viewModel.clearAllItems() }
        )
    }

    // 4. Staff Affinity Sheet
    if (uiState.showStaffAffinitySheet) {
        TierStaffAffinitySheet(
            config = uiState.config,
            onDismiss = { viewModel.closeStaffAffinitySheet() },
            onStaffClick = { name, isStudio ->
                viewModel.closeStaffAffinitySheet()
                if (isStudio) {
                    onNavigateToStudio(name)
                } else {
                    onNavigateToStaff(name)
                }
            }
        )
    }

    // 5. Correlation & Diagnosis Sheet
    if (uiState.showCorrelationSheet) {
        TierCorrelationSheet(
            config = uiState.config,
            onDismiss = { viewModel.closeCorrelationSheet() }
        )
    }

    // 6. Export Dialog
    if (uiState.showExportDialog) {
        TierExportDialog(
            onDismiss = { viewModel.closeExportDialog() },
            onExportImage = { options ->
                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val bitmap = createTierBoardBitmap(uiState.config, isEn, options, context)
                        viewModel.saveTierImageToGallery(bitmap)
                    } catch (e: Exception) {
                        viewModel.showSnackbar(if (isEn) "Failed to create image" else "画像の生成に失敗しました")
                    }
                }
            },
            onCopyImage = { options ->
                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val bitmap = createTierBoardBitmap(uiState.config, isEn, options, context)
                        viewModel.copyTierImageToClipboard(bitmap)
                    } catch (e: Exception) {
                        viewModel.showSnackbar(if (isEn) "Failed to copy image" else "画像のコピーに失敗しました")
                    }
                }
            },
            onCopyCleanText = {
                viewModel.copyCleanTierTextToClipboard()
            },
            onCopyMarkdown = {
                viewModel.copyMarkdownToClipboard()
            },
            onShareMarkdown = {
                viewModel.shareMarkdown(context)
            }
        )
    }
}

private fun splitTitleForCard(
    text: String,
    paint: android.graphics.Paint,
    maxWidth: Float
): Pair<String, String?> {
    if (paint.measureText(text) <= maxWidth) {
        return Pair(text, null)
    }
    // Find break point for line 1
    var break1 = 0
    for (i in 1..text.length) {
        if (paint.measureText(text.substring(0, i)) <= maxWidth) {
            break1 = i
        } else {
            break
        }
    }
    if (break1 == 0) break1 = 1
    val line1 = text.substring(0, break1)
    val remaining = text.substring(break1).trimStart()
    if (remaining.isEmpty()) {
        return Pair(line1, null)
    }
    if (paint.measureText(remaining) <= maxWidth) {
        return Pair(line1, remaining)
    }
    // Ellipsis for line 2
    val ellipsis = "…"
    val ellipsisW = paint.measureText(ellipsis)
    var break2 = 0
    for (i in 1..remaining.length) {
        if (paint.measureText(remaining.substring(0, i)) + ellipsisW <= maxWidth) {
            break2 = i
        } else {
            break
        }
    }
    val line2 = if (break2 > 0) remaining.substring(0, break2) + ellipsis else ellipsis
    return Pair(line1, line2)
}

private suspend fun createTierBoardBitmap(
    config: com.creditdb.pro.data.tier.TierTableConfig,
    isEn: Boolean,
    options: TierImageExportOptions,
    context: android.content.Context
): Bitmap {
    val s = options.scale
    val baseRowHeight = 156f
    val headerHeight = 56f * s
    val margin = 20f * s
    val rowSpacing = 12f * s
    val width = (1160f * s).toInt()

    val headerBoxWidth = 118f * s
    val cardWidth = 96f * s
    val cardHeight = baseRowHeight * s - 24f * s
    val cardSpacing = 12f * s
    val cardStartX = margin + headerBoxWidth + 16f * s
    val availableWidthForCards = (width - margin) - cardStartX
    val cardsPerLine = ((availableWidthForCards + cardSpacing) / (cardWidth + cardSpacing)).toInt().coerceAtLeast(1)

    fun calculateRowHeight(itemCount: Int): Float {
        if (itemCount == 0) return baseRowHeight * s
        val lines = ((itemCount - 1) / cardsPerLine) + 1
        return (24f * s + lines * cardHeight + (lines - 1) * cardSpacing)
    }

    val totalRowsHeight = config.rows.sumOf { calculateRowHeight(it.items.size).toDouble() }.toFloat()
    val totalHeight = (headerHeight + totalRowsHeight + (config.rows.size * rowSpacing) + margin + 10f * s).toInt().coerceAtLeast((400f * s).toInt())

    val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.parseColor("#121218"))

    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    // Load Gotham fonts
    val gothamBold: android.graphics.Typeface? = try {
        androidx.core.content.res.ResourcesCompat.getFont(context, com.creditdb.pro.R.font.gotham_bold)
    } catch (e: Exception) {
        null
    }
    val gothamBook: android.graphics.Typeface? = try {
        androidx.core.content.res.ResourcesCompat.getFont(context, com.creditdb.pro.R.font.gotham_book)
    } catch (e: Exception) {
        null
    }

    // 1. ウォーターマーク (右上: CreditDB のみ、フォントは GOTHAM Bold)
    paint.color = android.graphics.Color.WHITE
    paint.typeface = gothamBold ?: android.graphics.Typeface.DEFAULT_BOLD
    paint.textSize = 24f * s
    paint.isFakeBoldText = true
    paint.textAlign = android.graphics.Paint.Align.RIGHT
    canvas.drawText("CreditDB", width - margin - 4f * s, 38f * s, paint)

    // 2. 各Tier行の描画
    val imageLoader = coil.Coil.imageLoader(context)

    var currentY = headerHeight

    for (row in config.rows) {
        val rowColor = try {
            android.graphics.Color.parseColor(row.colorHex)
        } catch (e: Exception) {
            android.graphics.Color.GRAY
        }

        val thisRowHeight = calculateRowHeight(row.items.size)

        // 行背景カード
        paint.color = android.graphics.Color.parseColor("#1C1C26")
        paint.typeface = android.graphics.Typeface.DEFAULT
        canvas.drawRoundRect(margin, currentY, width - margin, currentY + thisRowHeight, 14f * s, 14f * s, paint)

        // Tierヘッダーボックス (パステル背景) - 行全体の高さに追従
        paint.color = rowColor
        canvas.drawRoundRect(margin, currentY, margin + headerBoxWidth, currentY + thisRowHeight, 14f * s, 14f * s, paint)
        canvas.drawRect(margin + headerBoxWidth - 16f * s, currentY, margin + headerBoxWidth, currentY + thisRowHeight, paint)

        // Tier文字: 背景色に対して視認性の高い黒色 (#121218) ＆ GOTHAM Bold (行の中央にセンタリング)
        paint.color = android.graphics.Color.parseColor("#121218")
        paint.typeface = gothamBold ?: android.graphics.Typeface.DEFAULT_BOLD
        paint.textSize = if (row.name.length > 2) 26f * s else 38f * s
        paint.isFakeBoldText = true
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText(row.name, margin + (headerBoxWidth / 2f), currentY + (thisRowHeight / 2f) + (13f * s), paint)

        // 作品カード群 (マルチライン段組み描画)
        paint.textAlign = android.graphics.Paint.Align.LEFT

        for ((index, item) in row.items.withIndex()) {
            val lineIdx = index / cardsPerLine
            val colIdx = index % cardsPerLine

            val cardX = cardStartX + colIdx * (cardWidth + cardSpacing)
            val cardY = currentY + 12f * s + lineIdx * (cardHeight + cardSpacing)
            val cardRect = android.graphics.RectF(cardX, cardY, cardX + cardWidth, cardY + cardHeight)

            // Coilからカバー画像Bitmapを取得
            var coverBitmap: Bitmap? = null
            if (!item.imageUrl.isNullOrBlank()) {
                try {
                    val req = coil.request.ImageRequest.Builder(context)
                        .data(item.imageUrl)
                        .allowHardware(false)
                        .build()
                    val result = imageLoader.execute(req)
                    val d = result.drawable
                    if (d is android.graphics.drawable.BitmapDrawable) {
                        coverBitmap = d.bitmap
                    }
                } catch (e: Exception) {
                    coverBitmap = null
                }
            }

            // カードベース下地描画
            paint.color = android.graphics.Color.parseColor("#252535")
            canvas.drawRoundRect(cardRect, 8f * s, 8f * s, paint)

            // カバー画像が取得できている場合は描画 (Center-Crop でアスペクト比を完全維持)
            if (coverBitmap != null) {
                val clipPath = android.graphics.Path().apply {
                    addRoundRect(cardRect, 8f * s, 8f * s, android.graphics.Path.Direction.CW)
                }
                canvas.save()
                canvas.clipPath(clipPath)

                val srcAspect = coverBitmap.width.toFloat() / coverBitmap.height.toFloat()
                val dstAspect = cardRect.width() / cardRect.height()

                val srcCropRect = if (srcAspect > dstAspect) {
                    // 横長画像: 左右両端を中央基準でトリミング
                    val cropWidth = (coverBitmap.height * dstAspect).toInt()
                    val left = ((coverBitmap.width - cropWidth) / 2).coerceAtLeast(0)
                    android.graphics.Rect(left, 0, (left + cropWidth).coerceAtMost(coverBitmap.width), coverBitmap.height)
                } else {
                    // 縦長画像: 上下両端を中央基準でトリミング
                    val cropHeight = (coverBitmap.width / dstAspect).toInt()
                    val top = ((coverBitmap.height - cropHeight) / 2).coerceAtLeast(0)
                    android.graphics.Rect(0, top, coverBitmap.width, (top + cropHeight).coerceAtMost(coverBitmap.height))
                }
                canvas.drawBitmap(coverBitmap, srcCropRect, cardRect, paint)

                // タイトルや年を表示する場合は下部にグラデーションシャドウを配置
                if (options.showTitle || options.showYear) {
                    val scrimHeight = cardHeight * 0.58f
                    val shader = android.graphics.LinearGradient(
                        cardRect.left, cardRect.bottom - scrimHeight,
                        cardRect.left, cardRect.bottom,
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.parseColor("#E60A0A10"),
                        android.graphics.Shader.TileMode.CLAMP
                    )
                    val scrimPaint = android.graphics.Paint().apply { this.shader = shader }
                    canvas.drawRect(cardRect.left, cardRect.bottom - scrimHeight, cardRect.right, cardRect.bottom, scrimPaint)
                }
                canvas.restore()
            }

            // 偏差値バッジ (チェックボックスがONの場合のみ描画)
            if (options.showDeviationScore) {
                paint.color = rowColor
                val badgeW = 44f * s
                val badgeH = 18f * s
                val badgeRect = android.graphics.RectF(cardRect.right - badgeW - 4f * s, cardRect.top + 4f * s, cardRect.right - 4f * s, cardRect.top + 4f * s + badgeH)
                canvas.drawRoundRect(badgeRect, 4f * s, 4f * s, paint)

                paint.color = android.graphics.Color.parseColor("#121218")
                paint.typeface = gothamBold ?: android.graphics.Typeface.DEFAULT_BOLD
                paint.textSize = 10.5f * s
                paint.isFakeBoldText = true
                paint.textAlign = android.graphics.Paint.Align.CENTER
                val devStr = String.format(java.util.Locale.US, "%.1f", item.deviationScore)
                canvas.drawText(devStr, badgeRect.centerX(), badgeRect.centerY() + 3.5f * s, paint)
            }

            // 作品名 & 放送年 (チェックボックスに応じて描画)
            val title = item.getDisplayTitle(isEn).first
            paint.textAlign = android.graphics.Paint.Align.LEFT
            paint.isFakeBoldText = true
            val maxTitleWidth = cardWidth - 10f * s

            if (options.showTitle && options.showYear) {
                paint.color = android.graphics.Color.WHITE
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                paint.textSize = 9.5f * s
                val (line1, line2) = splitTitleForCard(title, paint, maxTitleWidth)

                if (line2 != null) {
                    canvas.drawText(line1, cardRect.left + 5f * s, cardRect.bottom - 24f * s, paint)
                    canvas.drawText(line2, cardRect.left + 5f * s, cardRect.bottom - 14f * s, paint)
                } else {
                    canvas.drawText(line1, cardRect.left + 5f * s, cardRect.bottom - 15f * s, paint)
                }

                // 年代: 常に GOTHAM フォントで描画
                paint.color = android.graphics.Color.parseColor("#A4A4B4")
                paint.typeface = gothamBold ?: android.graphics.Typeface.DEFAULT_BOLD
                paint.textSize = 9.5f * s
                canvas.drawText("${item.year}", cardRect.left + 5f * s, cardRect.bottom - 4f * s, paint)
            } else if (options.showTitle) {
                paint.color = android.graphics.Color.WHITE
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                paint.textSize = 10f * s
                val (line1, line2) = splitTitleForCard(title, paint, maxTitleWidth)

                if (line2 != null) {
                    canvas.drawText(line1, cardRect.left + 5f * s, cardRect.bottom - 16f * s, paint)
                    canvas.drawText(line2, cardRect.left + 5f * s, cardRect.bottom - 5f * s, paint)
                } else {
                    canvas.drawText(line1, cardRect.left + 5f * s, cardRect.bottom - 7f * s, paint)
                }
            } else if (options.showYear) {
                // 年代のみ: 常に GOTHAM フォントで描画
                paint.color = android.graphics.Color.parseColor("#D0D0E0")
                paint.typeface = gothamBold ?: android.graphics.Typeface.DEFAULT_BOLD
                paint.textSize = 10.5f * s
                canvas.drawText("${item.year}", cardRect.left + 5f * s, cardRect.bottom - 6f * s, paint)
            } else if (coverBitmap == null) {
                paint.color = android.graphics.Color.WHITE
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                paint.textSize = 10f * s
                val (line1, line2) = splitTitleForCard(title, paint, maxTitleWidth)
                if (line2 != null) {
                    canvas.drawText(line1, cardRect.left + 5f * s, cardRect.centerY() - 2f * s, paint)
                    canvas.drawText(line2, cardRect.left + 5f * s, cardRect.centerY() + 10f * s, paint)
                } else {
                    canvas.drawText(line1, cardRect.left + 5f * s, cardRect.centerY() + 4f * s, paint)
                }
            }
        }

        currentY += thisRowHeight + rowSpacing
    }

    return bitmap
}

package com.creditdb.pro.ui.tier

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.creditdb.pro.data.CreditRepository
import com.creditdb.pro.data.WorkItem
import com.creditdb.pro.data.tier.*
import com.creditdb.pro.ui.theme.LanguageManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID

data class TierUiState(
    val config: TierTableConfig = TierTableConfig(),
    val expandedRowIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val searchResults: List<WorkItem> = emptyList(),
    val isSearching: Boolean = false,
    val showSearchSheet: Boolean = false,
    val targetRowIdForSearch: String? = null,
    val showCustomizationDialog: Boolean = false,
    val showStaffAffinitySheet: Boolean = false,
    val showCorrelationSheet: Boolean = false,
    val showExportDialog: Boolean = false,
    val selectedAnimeForAction: TierAnimeItem? = null,
    val selectedRowForAction: TierRowData? = null,
    val isDraggingAnimeId: String? = null,
    val snackbarMessage: String? = null
)

class TierViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CreditRepository(application)

    private val _uiState = MutableStateFlow(TierUiState())
    val uiState: StateFlow<TierUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        WorkImageResolver.initialize(application)
        loadSavedConfig()
    }

    private fun loadSavedConfig() {
        viewModelScope.launch {
            val loaded = TierStorageManager.loadConfig(getApplication())
            _uiState.update { it.copy(config = loaded) }
            preloadImagesForConfig(loaded)
        }
    }

    private fun persistConfig(newConfig: TierTableConfig) {
        _uiState.update { it.copy(config = newConfig) }
        TierStorageManager.saveConfig(getApplication(), newConfig)
        preloadImagesForConfig(newConfig)
    }

    private fun preloadImagesForConfig(config: TierTableConfig) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val app = getApplication<Application>()
            val imageLoader = coil.Coil.imageLoader(app)
            val urls = config.rows.flatMap { it.items }.mapNotNull { it.imageUrl }.distinct()
            for (url in urls) {
                val req = coil.request.ImageRequest.Builder(app)
                    .data(url)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build()
                imageLoader.enqueue(req)
            }
        }
    }

    fun toggleRowExpand(rowId: String) {
        _uiState.update { current ->
            val newSet = if (current.expandedRowIds.contains(rowId)) {
                current.expandedRowIds - rowId
            } else {
                current.expandedRowIds + rowId
            }
            current.copy(expandedRowIds = newSet)
        }
    }

    fun openSearchSheet(targetRowId: String? = null) {
        _uiState.update {
            it.copy(
                showSearchSheet = true,
                targetRowIdForSearch = targetRowId,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
        // Load popular/top anime initially
        searchAnime("")
    }

    fun closeSearchSheet() {
        _uiState.update { it.copy(showSearchSheet = false, targetRowIdForSearch = null) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250)
            searchAnime(query)
        }
    }

    private fun searchAnime(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val results = repository.getWorks(
                query = query,
                limit = 30
            )
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun addWorkToRow(work: WorkItem, targetRowId: String) {
        val currentConfig = _uiState.value.config
        val animeItem = WorkImageResolver.fromWorkItem(work, getApplication())

        // 1. 既存行にあれば削除（重複防止）
        val cleanedRows = currentConfig.rows.map { row ->
            row.copy(items = row.items.filterNot { it.id == animeItem.id })
        }

        // 2. 指定行へ追加
        val updatedRows = cleanedRows.map { row ->
            if (row.id == targetRowId) {
                row.copy(items = row.items + animeItem)
            } else {
                row
            }
        }

        persistConfig(currentConfig.copy(rows = updatedRows))
    }

    fun moveAnimeToRow(animeId: String, targetRowId: String) {
        val currentConfig = _uiState.value.config
        var itemToMove: TierAnimeItem? = null

        // 検索
        for (row in currentConfig.rows) {
            val found = row.items.find { it.id == animeId }
            if (found != null) {
                itemToMove = found
                break
            }
        }

        if (itemToMove == null) return

        val cleanedRows = currentConfig.rows.map { row ->
            row.copy(items = row.items.filterNot { it.id == animeId })
        }

        val updatedRows = cleanedRows.map { row ->
            if (row.id == targetRowId) {
                row.copy(items = row.items + itemToMove)
            } else {
                row
            }
        }

        persistConfig(currentConfig.copy(rows = updatedRows))
    }

    fun removeAnimeFromTier(animeId: String) {
        val currentConfig = _uiState.value.config
        val updatedRows = currentConfig.rows.map { row ->
            row.copy(items = row.items.filterNot { it.id == animeId })
        }
        persistConfig(currentConfig.copy(rows = updatedRows))
    }

    fun openAnimeActionSheet(anime: TierAnimeItem, row: TierRowData) {
        _uiState.update {
            it.copy(
                selectedAnimeForAction = anime,
                selectedRowForAction = row
            )
        }
    }

    fun closeAnimeActionSheet() {
        _uiState.update {
            it.copy(
                selectedAnimeForAction = null,
                selectedRowForAction = null
            )
        }
    }

    // --- Tier 設定カスタマイズ ---
    fun openCustomizationDialog() {
        _uiState.update { it.copy(showCustomizationDialog = true) }
    }

    fun closeCustomizationDialog() {
        _uiState.update { it.copy(showCustomizationDialog = false) }
    }

    fun reorderRows(fromIndex: Int, toIndex: Int) {
        val currentConfig = _uiState.value.config
        val rows = currentConfig.rows.toMutableList()
        if (fromIndex in rows.indices && toIndex in rows.indices) {
            val item = rows.removeAt(fromIndex)
            rows.add(toIndex, item)
            persistConfig(currentConfig.copy(rows = rows))
        }
    }

    fun updateRow(rowId: String, name: String, colorHex: String) {
        val currentConfig = _uiState.value.config
        val updatedRows = currentConfig.rows.map { row ->
            if (row.id == rowId) {
                row.copy(name = name, colorHex = colorHex)
            } else {
                row
            }
        }
        persistConfig(currentConfig.copy(rows = updatedRows))
    }

    fun addRow(name: String, colorHex: String) {
        val currentConfig = _uiState.value.config
        val newRow = TierRowData(
            id = UUID.randomUUID().toString(),
            name = name,
            colorHex = colorHex,
            items = emptyList()
        )
        persistConfig(currentConfig.copy(rows = currentConfig.rows + newRow))
    }

    fun deleteRow(rowId: String) {
        val currentConfig = _uiState.value.config
        val updatedRows = currentConfig.rows.filterNot { it.id == rowId }
        persistConfig(currentConfig.copy(rows = updatedRows))
    }

    fun resetToDefault() {
        val def = TierStorageManager.resetToDefault(getApplication())
        _uiState.update { it.copy(config = def, showCustomizationDialog = false) }
        showSnackbar(if (LanguageManager.isEnglish) "Reset to default tiers" else "デフォルトのTier構成に戻しました")
    }

    fun clearAllItems() {
        val cleared = TierStorageManager.clearAllItems(getApplication(), _uiState.value.config)
        _uiState.update { it.copy(config = cleared, showCustomizationDialog = false) }
        showSnackbar(if (LanguageManager.isEnglish) "Cleared all anime" else "すべての作品をクリアしました")
    }

    // --- 分析シート & エクスポート ---
    fun openStaffAffinitySheet() {
        _uiState.update { it.copy(showStaffAffinitySheet = true) }
    }

    fun closeStaffAffinitySheet() {
        _uiState.update { it.copy(showStaffAffinitySheet = false) }
    }

    fun openCorrelationSheet() {
        _uiState.update { it.copy(showCorrelationSheet = true) }
    }

    fun closeCorrelationSheet() {
        _uiState.update { it.copy(showCorrelationSheet = false) }
    }

    fun openExportDialog() {
        _uiState.update { it.copy(showExportDialog = true) }
    }

    fun closeExportDialog() {
        _uiState.update { it.copy(showExportDialog = false) }
    }

    fun harmonizeColors() {
        val currentConfig = _uiState.value.config
        val harmonizedRows = TierStorageManager.harmonizeRowColors(currentConfig.rows)
        persistConfig(currentConfig.copy(rows = harmonizedRows))
        showSnackbar(if (LanguageManager.isEnglish) "Harmonized tier colors" else "Tier行の色をグラデーションに自動整列しました")
    }

    fun copyCleanTierTextToClipboard() {
        val isEn = LanguageManager.isEnglish
        val config = _uiState.value.config
        val sb = StringBuilder()
        sb.appendLine(if (isEn) "# Anime Tier List" else "# アニメTier表")
        sb.appendLine()
        for (row in config.rows) {
            sb.appendLine("[ ${row.name} ]")
            if (row.items.isEmpty()) {
                sb.appendLine(if (isEn) "- (None)" else "- （なし）")
            } else {
                for (item in row.items) {
                    val title = item.getDisplayTitle(isEn).first
                    sb.appendLine("- $title (${item.year})")
                }
            }
            sb.appendLine()
        }
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Anime Tier List", sb.toString().trim())
        clipboard.setPrimaryClip(clip)
        showSnackbar(if (isEn) "Copied clean Tier List to clipboard" else "Tier表テキストをクリップボードにコピーしました")
    }

    fun copyTierImageToClipboard(bitmap: Bitmap) {
        val context = getApplication<Application>()
        val isEn = LanguageManager.isEnglish
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val cacheImagesDir = File(context.cacheDir, "images")
                if (!cacheImagesDir.exists()) cacheImagesDir.mkdirs()
                val imageFile = File(cacheImagesDir, "tier_clipboard_${System.currentTimeMillis()}.png")
                FileOutputStream(imageFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }

                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )

                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newUri(context.contentResolver, "Tier List Image", uri)
                clipboard.setPrimaryClip(clip)
                showSnackbar(if (isEn) "Copied image to clipboard!" else "画像をクリップボードにコピーしました！")
            } catch (e: Exception) {
                e.printStackTrace()
                showSnackbar(if (isEn) "Failed to copy image" else "画像のコピーに失敗しました")
            }
        }
    }

    fun copyMarkdownToClipboard() {
        val isEn = LanguageManager.isEnglish
        val md = TierAnalysisEngine.exportToMarkdown(_uiState.value.config, isEn)
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("CreditDB Tier AI Analysis", md)
        clipboard.setPrimaryClip(clip)
        showSnackbar(if (isEn) "Copied AI Markdown to clipboard" else "AI分析用Markdownをクリップボードにコピーしました")
    }

    fun shareMarkdown(context: Context) {
        val isEn = LanguageManager.isEnglish
        val md = TierAnalysisEngine.exportToMarkdown(_uiState.value.config, isEn)

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, md)
            putExtra(Intent.EXTRA_TITLE, "CreditDB_Tier_Analysis.md")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, if (isEn) "Share AI Markdown" else "AI分析用Markdownを共有")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    fun saveTierImageToGallery(bitmap: Bitmap) {
        val context = getApplication<Application>()
        val isEn = LanguageManager.isEnglish
        viewModelScope.launch {
            val success = saveBitmap(bitmap, context)
            if (success) {
                showSnackbar(if (isEn) "Saved Tier List image to Gallery!" else "Tier表の画像を端末に保存しました！")
            } else {
                showSnackbar(if (isEn) "Failed to save image" else "画像の保存に失敗しました")
            }
        }
    }

    private fun saveBitmap(bitmap: Bitmap, context: Context): Boolean {
        return try {
            val filename = "CreditDB_TierList_${System.currentTimeMillis()}.png"
            val fos: OutputStream?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CreditDB")
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/CreditDB"
                val file = File(imagesDir)
                if (!file.exists()) file.mkdirs()
                val image = File(imagesDir, filename)
                fos = FileOutputStream(image)
            }
            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun showSnackbar(msg: String) {
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}

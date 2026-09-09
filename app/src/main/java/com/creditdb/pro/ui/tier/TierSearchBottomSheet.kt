package com.creditdb.pro.ui.tier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.creditdb.pro.data.WorkItem
import com.creditdb.pro.data.tier.TierRowData
import com.creditdb.pro.data.tier.WorkImageResolver
import com.creditdb.pro.ui.theme.AppStrings
import com.creditdb.pro.ui.theme.LanguageManager
import com.creditdb.pro.ui.theme.TierTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TierSearchBottomSheet(
    allRows: List<TierRowData>,
    searchResults: List<WorkItem>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onAddWorkToRow: (work: WorkItem, targetRowId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val isEn = LanguageManager.isEnglish
    var query by remember { mutableStateOf("") }

    // Map of workId -> rowName for items already in the Tier table
    val alreadyAddedMap = remember(allRows) {
        val map = mutableMapOf<String, String>()
        for (row in allRows) {
            for (item in row.items) {
                map[item.id] = row.name
            }
        }
        map
    }

    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(view) {
        var parent = view.parent
        while (parent != null) {
            if (parent is androidx.compose.ui.window.DialogWindowProvider) {
                parent.window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                break
            }
            parent = parent.parent
        }
        onDispose {}
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        contentWindowInsets = { WindowInsets.navigationBars.union(WindowInsets.ime) },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = AppStrings.tierAddAnime(isEn),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Search Bar
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onQueryChange(it)
                },
                placeholder = {
                    Text(
                        text = AppStrings.tierSearchPlaceholder(isEn),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            onQueryChange("")
                        }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { keyboardController?.hide() }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else if (query.isNotBlank() && searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppStrings.tierSearchEmpty(isEn),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        items = searchResults,
                        key = { it.id }
                    ) { work ->
                        SearchResultCard(
                            work = work,
                            allRows = allRows,
                            alreadyInRowName = alreadyAddedMap[work.id],
                            onAddToRow = { rowId ->
                                onAddWorkToRow(work, rowId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    work: WorkItem,
    allRows: List<TierRowData>,
    alreadyInRowName: String?,
    onAddToRow: (rowId: String) -> Unit
) {
    val isEn = LanguageManager.isEnglish
    val (primaryTitle, secondaryTitle) = work.getDisplayTitle(isEn)
    val tierColorSpec = TierTheme.forTier(work.tier)
    val imageUrl = WorkImageResolver.getCoverImageUrl(work.id)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Poster
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = primaryTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp, 68.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Title + Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = primaryTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!secondaryTitle.isNullOrBlank()) {
                        Text(
                            text = secondaryTitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = tierColorSpec.onContainerColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, tierColorSpec.onContainerColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "偏差値 ${String.format(Locale.US, "%.1f", work.deviationScore)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = tierColorSpec.onContainerColor,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "${work.year}年",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        if (alreadyInRowName != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = alreadyInRowName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Add to Tier Buttons Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allRows) { row ->
                    val isSelected = row.name == alreadyInRowName
                    val rColor = try {
                        Color(android.graphics.Color.parseColor(row.colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    Surface(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onAddToRow(row.id) },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) rColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) rColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = "+ ${row.name}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) rColor else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

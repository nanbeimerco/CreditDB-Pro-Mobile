package com.creditdb.pro.ui.tier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.creditdb.pro.data.tier.TierRowData
import com.creditdb.pro.data.tier.TierStorageManager
import com.creditdb.pro.ui.theme.AppStrings
import com.creditdb.pro.ui.theme.LanguageManager

import androidx.compose.material.icons.filled.AutoAwesome

@Composable
fun TierCustomizationDialog(
    rows: List<TierRowData>,
    onDismiss: () -> Unit,
    onReorderRows: (fromIndex: Int, toIndex: Int) -> Unit,
    onUpdateRow: (rowId: String, name: String, colorHex: String) -> Unit,
    onAddRow: (name: String, colorHex: String) -> Unit,
    onDeleteRow: (rowId: String) -> Unit,
    onHarmonizeColors: () -> Unit,
    onResetToDefault: () -> Unit,
    onClearAllItems: () -> Unit
) {
    val isEn = LanguageManager.isEnglish
    var editingRowId by remember { mutableStateOf<String?>(null) }
    var newRowName by remember { mutableStateOf("") }
    var newRowColor by remember { mutableStateOf(TierStorageManager.COLOR_PRESETS.first()) }
    var isAddingNew by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.tierEditRows(isEn),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    FilledTonalButton(
                        onClick = onHarmonizeColors,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEn) "Sort Colors" else "色を自動整列", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(rows, key = { _, r -> r.id }) { index, row ->
                        RowConfigCard(
                            row = row,
                            canMoveUp = index > 0,
                            canMoveDown = index < rows.size - 1,
                            onMoveUp = { onReorderRows(index, index - 1) },
                            onMoveDown = { onReorderRows(index, index + 1) },
                            onDelete = { onDeleteRow(row.id) },
                            onColorChange = { hex -> onUpdateRow(row.id, row.name, hex) },
                            onNameChange = { name -> onUpdateRow(row.id, name, row.colorHex) }
                        )
                    }

                    if (isAddingNew) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    OutlinedTextField(
                                        value = newRowName,
                                        onValueChange = { newRowName = it },
                                        label = { Text(AppStrings.tierRowName(isEn)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(TierStorageManager.COLOR_PRESETS) { hex ->
                                            val c = Color(android.graphics.Color.parseColor(hex))
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(c)
                                                    .clickable { newRowColor = hex }
                                                    .then(
                                                        if (newRowColor == hex) Modifier.padding(2.dp)
                                                        else Modifier
                                                    )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { isAddingNew = false }) {
                                            Text(if (isEn) "Cancel" else "キャンセル")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                if (newRowName.isNotBlank()) {
                                                    onAddRow(newRowName.trim(), newRowColor)
                                                    newRowName = ""
                                                    isAddingNew = false
                                                }
                                            }
                                        ) {
                                            Text(if (isEn) "Add" else "追加")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            OutlinedButton(
                                onClick = { isAddingNew = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(AppStrings.tierAddRow(isEn))
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onResetToDefault,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(AppStrings.tierResetDefault(isEn), fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = onClearAllItems,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(AppStrings.tierClearAll(isEn), fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isEn) "Done" else "完了")
                }
            }
        }
    }
}

@Composable
private fun RowConfigCard(
    row: TierRowData,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onColorChange: (String) -> Unit,
    onNameChange: (String) -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var nameText by remember(row.name) { mutableStateOf(row.name) }
    val rColor = try {
        Color(android.graphics.Color.parseColor(row.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Color swatch
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(rColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = row.name.take(2),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Name field
                if (isEditingName) {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        onNameChange(nameText.trim())
                        isEditingName = false
                    }) {
                        Text("OK", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Text(
                        text = row.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isEditingName = true }
                    )
                }

                // Up / Down / Delete
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Up", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Down", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Color presets row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(TierStorageManager.COLOR_PRESETS) { hex ->
                    val c = Color(android.graphics.Color.parseColor(hex))
                    val isSelected = row.colorHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(c)
                            .clickable { onColorChange(hex) }
                            .then(
                                if (isSelected) Modifier.padding(2.dp)
                                else Modifier
                            )
                    )
                }
            }
        }
    }
}

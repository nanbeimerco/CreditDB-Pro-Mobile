package com.creditdb.pro.ui.tier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.creditdb.pro.data.tier.TierAnimeItem
import com.creditdb.pro.data.tier.TierRowData
import com.creditdb.pro.ui.theme.AppStrings
import com.creditdb.pro.ui.theme.LanguageManager
import com.creditdb.pro.ui.theme.TierTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TierAnimeActionSheet(
    anime: TierAnimeItem,
    currentRow: TierRowData,
    allRows: List<TierRowData>,
    onDismiss: () -> Unit,
    onMoveToRow: (targetRowId: String) -> Unit,
    onRemoveFromTier: () -> Unit,
    onViewDetails: () -> Unit
) {
    val isEn = LanguageManager.isEnglish
    val displayTitle = anime.getDisplayTitle(isEn).first
    val tierColorSpec = TierTheme.forTier(anime.tier)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header: Poster + Title + Deviation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!anime.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = anime.imageUrl,
                        contentDescription = displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp, 76.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = tierColorSpec.onContainerColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, tierColorSpec.onContainerColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "偏差値 ${String.format(Locale.US, "%.1f", anime.deviationScore)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = tierColorSpec.onContainerColor
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "${anime.year}年 | 現在: ${currentRow.name}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // Move to Tier Quick Select
            Text(
                text = AppStrings.tierMoveTo(isEn),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allRows) { row ->
                    val isCurrent = row.id == currentRow.id
                    val rColor = try {
                        Color(android.graphics.Color.parseColor(row.colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    Surface(
                        modifier = Modifier
                            .height(42.dp)
                            .defaultMinSize(minWidth = 56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !isCurrent) {
                                onMoveToRow(row.id)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCurrent) rColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            width = if (isCurrent) 2.dp else 1.dp,
                            color = if (isCurrent) rColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = row.name,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) rColor else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // View Work Details Button
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onViewDetails()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(AppStrings.tierTapToViewDetails(isEn))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Remove from Tier Button
            Button(
                onClick = {
                    onRemoveFromTier()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(AppStrings.tierRemoveFromList(isEn))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

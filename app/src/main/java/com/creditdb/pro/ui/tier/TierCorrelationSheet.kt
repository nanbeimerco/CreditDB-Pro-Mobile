package com.creditdb.pro.ui.tier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditdb.pro.data.tier.TierAnalysisEngine
import com.creditdb.pro.data.tier.TierTableConfig
import com.creditdb.pro.ui.theme.AppStrings
import com.creditdb.pro.ui.theme.LanguageManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TierCorrelationSheet(
    config: TierTableConfig,
    onDismiss: () -> Unit
) {
    val isEn = LanguageManager.isEnglish
    val result = remember(config) {
        TierAnalysisEngine.calculateTasteCorrelation(config)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = AppStrings.tierCorrelationTitle(isEn),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = AppStrings.tierCorrelationSubtitle(isEn),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // 1. Big Spearman Rho Hero Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isEn) "Spearman Rank Correlation (ρ)" else "スピアマン順位相関係数 (ρ)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val rhoStr = String.format(Locale.US, "%+.3f", result.spearmanRho)
                            val rhoColor = when {
                                result.spearmanRho >= 0.5 -> Color(0xFF8BD3A7)
                                result.spearmanRho >= 0.15 -> Color(0xFF8BC5E3)
                                result.spearmanRho >= -0.15 -> Color(0xFFA3B3E7)
                                else -> Color(0xFFDEA1A9)
                            }

                            Text(
                                text = if (result.sampleSize >= 3) rhoStr else "--",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 42.sp,
                                    color = rhoColor
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            val strengthText = when {
                                result.sampleSize < 3 -> if (isEn) "Insufficient Data" else "標本不足 (最低3作品)"
                                result.spearmanRho >= 0.6 -> if (isEn) "Strong Positive Correlation" else "強い正の相関（客観指標と連動）"
                                result.spearmanRho >= 0.2 -> if (isEn) "Moderate Positive Correlation" else "中程度の正の相関（バランス型）"
                                result.spearmanRho >= -0.2 -> if (isEn) "Low / Independent Correlation" else "無相関（完全独自路線）"
                                else -> if (isEn) "Negative / Inverse Correlation" else "負の相関（カルト・異色作志向）"
                            }

                            Surface(
                                color = rhoColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, rhoColor.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = strengthText,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = rhoColor
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isEn) "Ranked: ${result.sampleSize} anime" else "集計対象: ${result.sampleSize} 作品",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                // 2. Taste Diagnosis Profile Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isEn) result.diagnosisTitleEn else result.diagnosisTitleJa,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isEn) result.diagnosisDescEn else result.diagnosisDescJa,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                            )
                        }
                    }
                }

                // 3. Average Deviation Score per Tier
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (isEn) "Average Deviation Score per Tier" else "各Tierの平均偏差値",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            config.rows.forEach { row ->
                                val avg = result.avgDeviationPerTier[row.name] ?: 0.0
                                val count = result.countPerTier[row.name] ?: 0
                                val rColor = try {
                                    Color(android.graphics.Color.parseColor(row.colorHex))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Surface(
                                        color = rColor,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.width(36.dp)
                                    ) {
                                        Text(
                                            text = row.name,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            ),
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Progress Bar
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    ) {
                                        val progress = ((avg - 30.0) / 50.0).coerceIn(0.0, 1.0).toFloat()
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(progress)
                                                .clip(RoundedCornerShape(7.dp))
                                                .background(rColor)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = if (count > 0) "${String.format(Locale.US, "%.1f", avg)} (${count}作)" else "-",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        ),
                                        modifier = Modifier.width(72.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Mathematical Model Note
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isEn) {
                                    "CreditDB uses Bayesian shrinkage debiasing and release era Z-scores to calculate standard deviation scores (Mean=50, SD=10). This neutralizes rating inflation between 1980s classics and 2020s modern titles."
                                } else {
                                    "CreditDBの偏差値は、年代ごとのスコアインフレや投票規模の偏りをベイズ縮小推定量と年代内Zスコアで完全補正（平均50、標準偏差10）。1980年代の名作と2020年代の現代作を公平に横断比較しています。"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

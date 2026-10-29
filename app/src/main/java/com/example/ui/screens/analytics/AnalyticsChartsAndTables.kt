package com.example.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.i18n.TrStrings
import com.example.ui.components.BadgeVariant
import com.example.ui.components.KYBadge
import com.example.ui.components.KYCard
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.BorderLight
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.roundToInt

private val KYPrimary = PrimaryBlue
private val KYAccent = AccentAmber
private val KYBorder = BorderLight
private val KYSuccess = SuccessGreen
private val KYTextPrimary = TextPrimary
private val KYTextSecondary = TextSecondary

data class DailyVisitorPoint(
    val label: String,
    val fullDateLabel: String,
    val totalVisits: Int,
    val uniqueVisitors: Int
)

data class CatalogVisitorBar(
    val catalogId: String,
    val shortTitle: String,
    val fullTitle: String,
    val totalVisits: Int,
    val uniqueVisitors: Int,
    val orderCount: Int
)

data class ProductAnalyticsRow(
    val productId: String,
    val productName: String,
    val imageUrl: String,
    val catalogTitle: String,
    val price: Double,
    val currency: String,
    val viewCount: Int,
    val addToCartCount: Int,
    val orderCount: Int
)

data class ReferrerStat(
    val key: String,
    val label: String,
    val count: Int,
    val percentage: Float,
    val color: Color
)

/**
 * Grafik 1: Günlük ziyaretçi çizgi grafiği (Recharts stilinde etkileşimli Canvas çizgi/alan grafiği).
 */
@Composable
fun DailyVisitorsLineChartCard(
    points: List<DailyVisitorPoint>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(points) {
        mutableIntStateOf((points.size - 1).coerceAtLeast(0))
    }
    val safeSelectedIdx = selectedIndex.coerceIn(0, (points.size - 1).coerceAtLeast(0))
    val selectedPoint = points.getOrNull(safeSelectedIdx)

    val maxVal = remember(points) {
        (points.maxOfOrNull { maxOf(it.totalVisits, it.uniqueVisitors) } ?: 10).coerceAtLeast(5)
    }

    KYCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chart_daily_visitors_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(KYPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = KYPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = TrStrings.Analytics.CHART_DAILY_VISITORS_TITLE,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = KYTextPrimary
                    )
                    Text(
                        text = TrStrings.Analytics.CHART_DAILY_VISITORS_SUBTITLE,
                        style = MaterialTheme.typography.bodySmall,
                        color = KYTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Seçili gün bilgi kartı (Recharts Tooltip karşılığı)
        if (selectedPoint != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, KYBorder, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedPoint.fullDateLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = KYTextPrimary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(KYPrimary)
                            )
                            Text(
                                text = "${selectedPoint.totalVisits} ziyaret",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = KYPrimary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(KYAccent)
                            )
                            Text(
                                text = "${selectedPoint.uniqueVisitors} tekil",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Y-Ekseni Etiketleri + Çizgi Grafiği
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Y-Axis değer sütunu
            Column(
                modifier = Modifier
                    .width(30.dp)
                    .fillMaxHeight()
                    .padding(bottom = 22.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = maxVal.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = KYTextSecondary
                )
                Text(
                    text = (maxVal * 0.66f).roundToInt().toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = KYTextSecondary
                )
                Text(
                    text = (maxVal * 0.33f).roundToInt().toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = KYTextSecondary
                )
                Text(
                    text = "0",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = KYTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(points) {
                            detectTapGestures { tapOffset ->
                                if (points.isNotEmpty() && size.width > 0) {
                                    val stepX = if (points.size > 1) {
                                        size.width.toFloat() / (points.size - 1).toFloat()
                                    } else {
                                        size.width.toFloat()
                                    }
                                    val nearest = points.indices.minByOrNull { idx ->
                                        val px = if (points.size > 1) idx * stepX else size.width / 2f
                                        abs(px - tapOffset.x)
                                    } ?: 0
                                    selectedIndex = nearest
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                        // Yatay 4 kılavuz çizgisi
                        for (i in 0..3) {
                            val y = h * (i / 3f)
                            drawLine(
                                color = Color(0xFFE2E8F0),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = if (i < 3) dashEffect else null
                            )
                        }

                        if (points.isEmpty()) return@Canvas

                        val count = points.size
                        val stepX = if (count > 1) w / (count - 1).toFloat() else w / 2f

                        val totalCoords = points.mapIndexed { idx, pt ->
                            val x = if (count > 1) idx * stepX else w / 2f
                            val ratio = (pt.totalVisits.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                            val y = h - (ratio * (h - 12.dp.toPx())) - 4.dp.toPx()
                            Offset(x, y)
                        }

                        val uniqueCoords = points.mapIndexed { idx, pt ->
                            val x = if (count > 1) idx * stepX else w / 2f
                            val ratio = (pt.uniqueVisitors.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                            val y = h - (ratio * (h - 12.dp.toPx())) - 4.dp.toPx()
                            Offset(x, y)
                        }

                        // Alan dolgusu (Toplam Ziyaret)
                        if (totalCoords.size > 1) {
                            val areaPath = Path().apply {
                                moveTo(totalCoords.first().x, h)
                                totalCoords.forEach { lineTo(it.x, it.y) }
                                lineTo(totalCoords.last().x, h)
                                close()
                            }
                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        KYPrimary.copy(alpha = 0.25f),
                                        KYPrimary.copy(alpha = 0.02f)
                                    ),
                                    startY = 0f,
                                    endY = h
                                )
                            )
                        }

                        // Benzersiz Ziyaretçi çizgisi (#F59E0B)
                        if (uniqueCoords.size > 1) {
                            val uniquePath = Path().apply {
                                moveTo(uniqueCoords.first().x, uniqueCoords.first().y)
                                for (i in 1 until uniqueCoords.size) {
                                    lineTo(uniqueCoords[i].x, uniqueCoords[i].y)
                                }
                            }
                            drawPath(
                                path = uniquePath,
                                color = KYAccent,
                                style = Stroke(
                                    width = 2.2.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // Toplam Ziyaret çizgisi (#2563EB)
                        if (totalCoords.size > 1) {
                            val linePath = Path().apply {
                                moveTo(totalCoords.first().x, totalCoords.first().y)
                                for (i in 1 until totalCoords.size) {
                                    lineTo(totalCoords[i].x, totalCoords[i].y)
                                }
                            }
                            drawPath(
                                path = linePath,
                                color = KYPrimary,
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // Seçili nokta dikey kılavuz çizgisi
                        val selCoord = totalCoords.getOrNull(safeSelectedIdx)
                        if (selCoord != null) {
                            drawLine(
                                color = KYPrimary.copy(alpha = 0.35f),
                                start = Offset(selCoord.x, 0f),
                                end = Offset(selCoord.x, h),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = dashEffect
                            )
                        }

                        // Veri noktaları
                        totalCoords.forEachIndexed { idx, offset ->
                            val isSelected = idx == safeSelectedIdx
                            if (count <= 14 || isSelected || idx == 0 || idx == count - 1) {
                                drawCircle(
                                    color = Color.White,
                                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                                    center = offset
                                )
                                drawCircle(
                                    color = KYPrimary,
                                    radius = if (isSelected) 4.5.dp.toPx() else 2.8.dp.toPx(),
                                    center = offset
                                )
                            }
                        }

                        uniqueCoords.forEachIndexed { idx, offset ->
                            val isSelected = idx == safeSelectedIdx
                            if (isSelected) {
                                drawCircle(
                                    color = Color.White,
                                    radius = 5.dp.toPx(),
                                    center = offset
                                )
                                drawCircle(
                                    color = KYAccent,
                                    radius = 3.5.dp.toPx(),
                                    center = offset
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // X-Ekseni Tarih Etiketleri (Kalabalık olmaması için en fazla 6 etiket göster)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val labelIndices = remember(points.size) {
                        if (points.size <= 7) {
                            points.indices.toList()
                        } else {
                            val step = ((points.size - 1) / 5f).roundToInt().coerceAtLeast(1)
                            (0 until points.size step step).toList().let { list ->
                                if (list.lastOrNull() != points.lastIndex) list + points.lastIndex else list
                            }
                        }
                    }
                    labelIndices.forEach { idx ->
                        val pt = points[idx]
                        Text(
                            text = pt.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (idx == safeSelectedIdx) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (idx == safeSelectedIdx) KYPrimary else KYTextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grafik Gösterge (Legend)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(KYPrimary)
                    )
                    Text(
                        text = TrStrings.Analytics.LEGEND_TOTAL_VISITS,
                        style = MaterialTheme.typography.labelSmall,
                        color = KYTextSecondary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(KYAccent)
                    )
                    Text(
                        text = TrStrings.Analytics.LEGEND_UNIQUE_VISITORS,
                        style = MaterialTheme.typography.labelSmall,
                        color = KYTextSecondary
                    )
                }
            }
            Text(
                text = TrStrings.Analytics.CHART_TAP_HINT,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = KYTextSecondary
            )
        }
    }
}

/**
 * Grafik 2: Katalog bazında ziyaretçi çubuk grafiği.
 */
@Composable
fun CatalogVisitorsBarChartCard(
    bars: List<CatalogVisitorBar>,
    modifier: Modifier = Modifier
) {
    var selectedBarIdx by remember(bars) { mutableIntStateOf(0) }
    val safeSelectedIdx = selectedBarIdx.coerceIn(0, (bars.size - 1).coerceAtLeast(0))
    val selectedBar = bars.getOrNull(safeSelectedIdx)
    val maxVisits = remember(bars) {
        (bars.maxOfOrNull { it.totalVisits } ?: 10).coerceAtLeast(5)
    }

    val barPalette = listOf(
        KYPrimary,
        Color(0xFF3B82F6),
        KYAccent,
        KYSuccess,
        Color(0xFF8B5CF6)
    )

    KYCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chart_catalog_bar_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(KYAccent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = TrStrings.Analytics.CHART_CATALOG_BAR_TITLE,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = KYTextPrimary
                )
                Text(
                    text = TrStrings.Analytics.CHART_CATALOG_BAR_SUBTITLE,
                    style = MaterialTheme.typography.bodySmall,
                    color = KYTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedBar != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, KYBorder, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedBar.fullTitle,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = KYTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${selectedBar.totalVisits} ziyaret • ${selectedBar.orderCount} sipariş",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = KYPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Canvas Çubuk Grafiği
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(165.dp)
                .pointerInput(bars) {
                    detectTapGestures { tapOffset ->
                        if (bars.isNotEmpty() && size.width > 0) {
                            val slotWidth = size.width.toFloat() / bars.size.toFloat()
                            val idx = (tapOffset.x / slotWidth).toInt().coerceIn(0, bars.lastIndex)
                            selectedBarIdx = idx
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                for (i in 0..3) {
                    val y = h * (i / 3f)
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (i < 3) dashEffect else null
                    )
                }

                if (bars.isEmpty()) return@Canvas

                val slotWidth = w / bars.size.toFloat()
                val barWidth = (slotWidth * 0.46f).coerceAtMost(46.dp.toPx())

                bars.forEachIndexed { idx, bar ->
                    val ratio = (bar.totalVisits.toFloat() / maxVisits.toFloat()).coerceIn(0.06f, 1f)
                    val barHeight = ratio * (h - 14.dp.toPx())
                    val left = (idx * slotWidth) + (slotWidth - barWidth) / 2f
                    val top = h - barHeight
                    val isSelected = idx == safeSelectedIdx
                    val baseColor = barPalette[idx % barPalette.size]

                    // Seçili sütun arka plan vurgusu
                    if (isSelected) {
                        drawRoundRect(
                            color = baseColor.copy(alpha = 0.08f),
                            topLeft = Offset(idx * slotWidth + 4.dp.toPx(), 0f),
                            size = Size(slotWidth - 8.dp.toPx(), h),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    }

                    // Ana Çubuk
                    drawRoundRect(
                        color = if (isSelected) baseColor else baseColor.copy(alpha = 0.78f),
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Çubuk Altı Katalog Etiketleri ve Sayılar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            bars.forEachIndexed { idx, bar ->
                val isSelected = idx == safeSelectedIdx
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedBarIdx = idx }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${bar.totalVisits}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) KYPrimary else KYTextPrimary
                    )
                    Text(
                        text = bar.shortTitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) KYPrimary else KYTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * "En çok görüntülenen ürünler" (Top 10) ve "En çok sepete eklenen ürünler" listeleri.
 */
@Composable
fun TopProductsSectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    items: List<ProductAnalyticsRow>,
    metricSelector: (ProductAnalyticsRow) -> Int,
    unitLabel: String,
    onProductClick: (String) -> Unit,
    testTag: String
) {
    val maxMetric = remember(items) {
        (items.maxOfOrNull { metricSelector(it) } ?: 1).coerceAtLeast(1)
    }

    KYCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = KYTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = KYTextSecondary
                )
            }
            KYBadge(
                text = "Top ${items.size}",
                variant = BadgeVariant.PRIMARY
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEachIndexed { idx, row ->
                val value = metricSelector(row)
                val ratio = (value.toFloat() / maxMetric.toFloat()).coerceIn(0.06f, 1f)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .clickable { onProductClick(row.productId) }
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Sıralama rozeti (#1 .. #10)
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(
                                    if (idx < 3) accentColor else Color(0xFFE2E8F0)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (idx < 3) Color.White else KYTextPrimary
                            )
                        }

                        AsyncImage(
                            model = row.imageUrl,
                            contentDescription = row.productName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE2E8F0))
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.productName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = KYTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = row.catalogTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = KYTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$value",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                            Text(
                                text = unitLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = KYTextSecondary
                            )
                        }
                    }

                    // İlerleme çubuğu
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio)
                                .height(5.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(accentColor)
                        )
                    }
                }
            }
        }
    }
}

/**
 * "Müşteri ilgisi" bölümü: ürün başına görüntülenme, sepete ekleme, sipariş sayıları tablo halinde.
 */
@Composable
fun CustomerInterestTableCard(
    rows: List<ProductAnalyticsRow>,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAll by remember { mutableStateOf(false) }
    val displayedRows = remember(rows, showAll) {
        if (showAll) rows else rows.take(8)
    }

    KYCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("customer_interest_table_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(KYSuccess.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TableChart,
                    contentDescription = null,
                    tint = KYSuccess,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = TrStrings.Analytics.CUSTOMER_INTEREST_TITLE,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = KYTextPrimary
                )
                Text(
                    text = TrStrings.Analytics.CUSTOMER_INTEREST_SUBTITLE,
                    style = MaterialTheme.typography.bodySmall,
                    color = KYTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tablo Başlık Satırı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = TrStrings.Analytics.TABLE_COL_PRODUCT,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = KYTextSecondary,
                modifier = Modifier.weight(1.9f)
            )
            Text(
                text = TrStrings.Analytics.TABLE_COL_VIEWS,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = KYTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.95f)
            )
            Text(
                text = TrStrings.Analytics.TABLE_COL_CART,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = KYTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.95f)
            )
            Text(
                text = TrStrings.Analytics.TABLE_COL_ORDERS,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = KYTextSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.7f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Tablo Satırları
        displayedRows.forEachIndexed { idx, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProductClick(item.productId) }
                    .padding(horizontal = 8.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1.9f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.productName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE2E8F0))
                    )
                    Column {
                        Text(
                            text = item.productName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = KYTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.price.toInt()} ${item.currency}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = KYTextSecondary
                        )
                    }
                }

                // Görüntülenme
                Row(
                    modifier = Modifier.weight(0.95f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = KYPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${item.viewCount}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = KYTextPrimary
                    )
                }

                // Sepete Ekleme
                Row(
                    modifier = Modifier.weight(0.95f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${item.addToCartCount}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFD97706)
                    )
                }

                // Sipariş Sayısı
                Row(
                    modifier = Modifier.weight(0.7f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = KYSuccess,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${item.orderCount}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = KYSuccess
                    )
                }
            }

            if (idx < displayedRows.lastIndex) {
                HorizontalDivider(color = KYBorder.copy(alpha = 0.6f))
            }
        }

        if (rows.size > 8) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8FAFC))
                    .clickable { showAll = !showAll }
                    .padding(vertical = 10.dp)
                    .testTag("toggle_customer_interest_rows"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (showAll) {
                        TrStrings.Analytics.BTN_SHOW_LESS_PRODUCTS
                    } else {
                        "${TrStrings.Analytics.BTN_SHOW_ALL_PRODUCTS} (${rows.size})"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = KYPrimary
                )
            }
        }
    }
}

/**
 * Kaynak dağılımı (Referrer): WhatsApp, Doğrudan link, Diğer.
 */
@Composable
fun ReferrerDistributionCard(
    stats: List<ReferrerStat>,
    totalVisits: Int,
    modifier: Modifier = Modifier
) {
    KYCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("referrer_distribution_card")
    ) {
        Text(
            text = TrStrings.Analytics.REFERRER_TITLE,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = KYTextPrimary
        )
        Text(
            text = TrStrings.Analytics.REFERRER_SUBTITLE,
            style = MaterialTheme.typography.bodySmall,
            color = KYTextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Yığılmış (Stacked) İlerleme Çubuğu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFE2E8F0)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            stats.forEach { stat ->
                val weight = stat.percentage.coerceAtLeast(4f)
                Box(
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .background(stat.color)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            stats.forEach { stat ->
                val icon = when (stat.key) {
                    "whatsapp" -> Icons.Default.ChatBubbleOutline
                    "direct" -> Icons.Default.Link
                    else -> Icons.Default.Public
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(stat.color.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = stat.color,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stat.label,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = KYTextPrimary
                            )
                            Text(
                                text = "${stat.count} ziyaretçi",
                                style = MaterialTheme.typography.labelSmall,
                                color = KYTextSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = stat.color.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "%${stat.percentage.roundToInt()}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = stat.color,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

package com.gh.healthapp.ui.walk

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gh.healthapp.walk.GeoPoint
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/** 지도 타일 없이 경로 모양만 그리는 가벼운 썸네일 */
@Composable
fun RouteThumbnail(points: List<GeoPoint>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val startColor = MaterialTheme.colorScheme.secondary
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (points.size < 2) {
            Text("경로 없음", style = MaterialTheme.typography.labelSmall)
            return@Box
        }
        Canvas(Modifier.fillMaxSize()) {
            val pad = 8.dp.toPx()
            val midLat = (points.maxOf { it.lat } + points.minOf { it.lat }) / 2.0
            val k = cos(Math.toRadians(midLat)) // 경도 보정
            val xs = points.map { it.lon * k }
            val ys = points.map { -it.lat } // 위도는 위쪽이 북쪽
            val minX = xs.min(); val maxX = xs.max()
            val minY = ys.min(); val maxY = ys.max()
            val spanX = max(maxX - minX, 1e-9)
            val spanY = max(maxY - minY, 1e-9)
            val scale = min((size.width - 2 * pad) / spanX, (size.height - 2 * pad) / spanY).toFloat()
            val offX = (size.width - (spanX * scale).toFloat()) / 2f
            val offY = (size.height - (spanY * scale).toFloat()) / 2f
            fun pos(i: Int) = Offset(
                offX + ((xs[i] - minX) * scale).toFloat(),
                offY + ((ys[i] - minY) * scale).toFloat(),
            )
            val path = Path()
            points.indices.forEach { i ->
                val p = pos(i)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, lineColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(startColor, 4.dp.toPx(), pos(0))
            drawCircle(lineColor, 4.dp.toPx(), pos(points.lastIndex))
        }
    }
}

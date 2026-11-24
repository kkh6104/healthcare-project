package com.gh.healthapp.ui.map

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gh.healthapp.walk.GeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File
import org.osmdroid.util.GeoPoint as OsmPoint

private val SEOUL_CITY_HALL = OsmPoint(37.5663, 126.9779)

/** OpenStreetMap(osmdroid) 경로 지도. 나중에 다른 지도로 바꿀 때 이 함수만 교체 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun RouteMap(
    points: List<GeoPoint>,
    modifier: Modifier = Modifier,
    followLast: Boolean = false,
    interactive: Boolean = true,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val lifecycleOwner = LocalLifecycleOwner.current
    val lineColor = MaterialTheme.colorScheme.primary.toArgb()
    val startColor = MaterialTheme.colorScheme.secondary.toArgb()
    val ringColor = MaterialTheme.colorScheme.surface.toArgb()

    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName // OSM 타일 사용 정책
            val base = File(context.cacheDir, "osmdroid")
            osmdroidBasePath = base
            osmdroidTileCache = File(base, "tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 3.0
            controller.setZoom(12.0)
            controller.setCenter(SEOUL_CITY_HALL)
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        // 이미 RESUMED 상태에서 들어온 경우
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Box(modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                map.setMultiTouchControls(interactive)
                map.setOnTouchListener(if (interactive) null else View.OnTouchListener { _, _ -> true })

                map.overlays.clear()
                val pts = points.map { OsmPoint(it.lat, it.lon) }
                if (pts.size >= 2) {
                    map.overlays.add(
                        Polyline(map).apply {
                            setPoints(pts)
                            outlinePaint.color = lineColor
                            outlinePaint.strokeWidth = 6f * density
                            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        }
                    )
                }
                if (pts.isNotEmpty()) {
                    map.overlays.add(dot(map, pts.first(), startColor, ringColor, 12f * density))
                    if (pts.size >= 2) map.overlays.add(dot(map, pts.last(), lineColor, ringColor, 14f * density))
                }

                when {
                    pts.isEmpty() -> {
                        map.controller.setZoom(12.0)
                        map.controller.setCenter(SEOUL_CITY_HALL)
                    }
                    followLast || pts.size == 1 || isTiny(pts) -> {
                        if (map.zoomLevelDouble < 16.5 || !followLast) map.controller.setZoom(17.0)
                        map.controller.setCenter(pts.last())
                    }
                    else -> {
                        val box = BoundingBox.fromGeoPoints(pts)
                        map.post { map.zoomToBoundingBox(box, false, (48 * density).toInt()) }
                    }
                }
                map.invalidate()
            },
        )
        if (points.isEmpty()) {
            Text(
                "위치를 찾는 중",
                Modifier.align(Alignment.Center)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            "© OpenStreetMap contributors",
            Modifier.align(Alignment.BottomEnd)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .padding(horizontal = 4.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private fun isTiny(pts: List<OsmPoint>): Boolean {
    val lat = pts.maxOf { it.latitude } - pts.minOf { it.latitude }
    val lon = pts.maxOf { it.longitude } - pts.minOf { it.longitude }
    return lat < 1e-5 && lon < 1e-5
}

/** 테두리가 있는 원형 표시 */
private fun dot(map: MapView, p: OsmPoint, color: Int, ring: Int, sizePx: Float): Marker =
    Marker(map).apply {
        position = p
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        icon = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke((sizePx / 5f).toInt().coerceAtLeast(2), ring)
            setSize(sizePx.toInt(), sizePx.toInt())
        }
        setInfoWindow(null)
    }

package com.gh.healthapp.ui.walk

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gh.healthapp.data.HealthDatabase
import com.gh.healthapp.data.WalkSessionEntity
import com.gh.healthapp.ui.map.RouteMap
import com.gh.healthapp.walk.GeoPoint
import com.gh.healthapp.walk.WalkPermissions
import com.gh.healthapp.walk.WalkStatus
import com.gh.healthapp.walk.WalkSettings
import com.gh.healthapp.walk.WalkTracker
import com.gh.healthapp.walk.formatElapsed
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("M월 d일 (E) a h:mm", Locale.KOREAN).withZone(ZoneId.systemDefault())

/** 걷기 탭: 쉬는 중이면 시작 화면 + 최근 기록, 기록 중이면 지도 + 통계 */
@Composable
fun WalkTab(onOpenWalk: (Long) -> Unit) {
    val live by WalkTracker.state.collectAsState()
    if (live.countdown != null) {
        WalkCountdown(live.countdown!!)
    } else if (live.status == WalkStatus.IDLE) {
        WalkIdle(onOpenWalk)
    } else {
        WalkRunning()
    }
}

@Composable
private fun WalkIdle(onOpenWalk: (Long) -> Unit) {
    val context = LocalContext.current
    val live by WalkTracker.state.collectAsState()
    val settings = remember(context) { WalkSettings(context) }
    val settingsState by settings.state.collectAsState()
    val walkDao = remember { HealthDatabase.get(context).walkDao() }
    val sessions by walkDao.sessionsFlow().collectAsState(initial = emptyList())
    var denied by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        // 위치 권한이 있어야 시작할 수 있다 (알림/걸음 센서 권한은 없어도 기록은 가능)
        if (WalkPermissions.hasLocation(context)) {
            denied = false
            WalkTracker.startWithCountdown(context)
        } else {
            denied = true
        }
    }

    val zone = ZoneId.systemDefault()
    val weekStart = LocalDate.now(zone).with(DayOfWeek.MONDAY).atStartOfDay(zone).toInstant().toEpochMilli()
    val week = sessions.filter { it.startTime >= weekStart }
    val weekKm = week.sumOf { it.distanceM } / 1000.0

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Button(
                onClick = {
                    val missing = WalkPermissions.missing(context)
                    if (missing.isEmpty()) { denied = false; WalkTracker.startWithCountdown(context) } else launcher.launch(missing.toTypedArray())
                },
                modifier = Modifier.fillMaxWidth().height(72.dp),
            ) { Text("걷기 시작", style = MaterialTheme.typography.titleLarge) }
        }
        item {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("자동 일시정지", Modifier.weight(1f))
                    Switch(checked = settingsState.autoPause, onCheckedChange = settings::setAutoPause)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("이어폰 음성 안내", Modifier.weight(1f))
                    Switch(checked = settingsState.voice, onCheckedChange = settings::setVoice)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("1km마다 음성 안내", Modifier.weight(1f))
                    Switch(checked = settingsState.kmAnnounce, onCheckedChange = settings::setKmAnnounce)
                }
            }
        }
        if (denied) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "위치 권한이 필요합니다. 설정에서 이 앱의 위치 권한을 허용해 주세요.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        )
                    }) { Text("설정 열기") }
                }
            }
        }
        item {
            Text(
                "이번 주 ${week.size}회 · " + String.format(Locale.KOREAN, "%.1f km", weekKm),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (sessions.isEmpty()) {
            item { Text("아직 걷기 기록이 없습니다.", style = MaterialTheme.typography.bodyMedium) }
        }
        items(sessions, key = { it.id }) { s ->
            SessionRow(s) { onOpenWalk(s.id) }
        }
    }
}

@Composable
private fun WalkCountdown(value: Int) {
    BackHandler { WalkTracker.cancelCountdown() }
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.88f)) {}
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            AnimatedContent(
                targetState = value,
                transitionSpec = { (scaleIn(initialScale = 1.5f) + fadeIn()).togetherWith(scaleOut(targetScale = 0.6f) + fadeOut()) },
                label = "walk countdown",
            ) { number ->
                Text("$number", style = MaterialTheme.typography.displayLarge, color = Color.White)
            }
            OutlinedButton(onClick = { WalkTracker.cancelCountdown() }) { Text("취소") }
        }
    }
}

@Composable
private fun SessionRow(s: WalkSessionEntity, onClick: () -> Unit) {
    val context = LocalContext.current
    val fmt = NumberFormat.getInstance(Locale.KOREA)
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(dateFmt.format(Instant.ofEpochMilli(s.startTime)), style = MaterialTheme.typography.titleSmall)
                Text(formatKm(s.distanceM), style = MaterialTheme.typography.titleLarge)
                Text(
                    "${formatElapsed(s.activeMs)} · 페이스 ${formatPace(s.distanceM, s.activeMs)} · 걸음 ${fmt.format(s.steps)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            val points by produceState(emptyList<GeoPoint>(), s.id) {
                value = HealthDatabase.get(context).walkDao().points(s.id).map { GeoPoint(it.lat, it.lon) }
            }
            RouteThumbnail(points, Modifier.width(120.dp).height(120.dp))
        }
    }
}

@Composable
private fun WalkRunning() {
    val context = LocalContext.current
    val live by WalkTracker.state.collectAsState()
    val fmt = NumberFormat.getInstance(Locale.KOREA)
    var confirmStop by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        if (live.inVehicle || live.status == WalkStatus.AUTO_PAUSED) {
            Surface(
                modifier = Modifier.padding(8.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    if (live.inVehicle) "이동 중 (걸음 없음) · 거리 제외" else "자동 일시정지됨 · 걸으면 다시 시작",
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        RouteMap(live.points, Modifier.fillMaxWidth().weight(1f).clipToBounds(), followLast = true)
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                (if (live.status == WalkStatus.PAUSED) "일시정지 · " else "") + formatKm(live.distanceM),
                style = MaterialTheme.typography.displaySmall,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("시간", formatElapsed(live.activeMs))
                Stat("페이스", formatPace(live.distanceM, live.activeMs))
                Stat("걸음", fmt.format(live.steps))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (live.status == WalkStatus.PAUSED) {
                    Button(onClick = { WalkTracker.resume(context) }, Modifier.weight(1f)) { Text("재개") }
                } else {
                    OutlinedButton(onClick = { WalkTracker.pause(context) }, Modifier.weight(1f)) { Text("일시정지") }
                }
                Button(onClick = { confirmStop = true }, Modifier.weight(1f)) { Text("종료") }
            }
        }
    }
    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("걷기를 종료할까요?") },
            text = { Text("지금까지의 기록이 저장됩니다.") },
            confirmButton = {
                TextButton(onClick = { confirmStop = false; WalkTracker.stop(context) }) { Text("종료") }
            },
            dismissButton = { TextButton(onClick = { confirmStop = false }) { Text("계속 걷기") } },
        )
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

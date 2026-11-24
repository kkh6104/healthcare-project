package com.gh.healthapp.ui.walk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gh.healthapp.data.HealthDatabase
import com.gh.healthapp.data.WalkSessionEntity
import com.gh.healthapp.ui.map.RouteMap
import com.gh.healthapp.walk.GeoPoint
import com.gh.healthapp.walk.RawPoint
import com.gh.healthapp.walk.smoothRoute
import com.gh.healthapp.walk.WalkService
import com.gh.healthapp.walk.formatElapsed
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val zone: ZoneId get() = ZoneId.systemDefault()
private val dayFmt = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
private val timeFmt = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

/** 걷기 기록 상세: 전체 경로 지도 + 요약 + 삭제 */
@Composable
fun WalkDetail(sessionId: Long, onDeleted: () -> Unit) {
    val context = LocalContext.current
    val db = remember { HealthDatabase.get(context) }
    val scope = rememberCoroutineScope()
    val session by produceState<WalkSessionEntity?>(null, sessionId) { value = db.walkDao().session(sessionId) }
    val points by produceState(emptyList<GeoPoint>(), sessionId) {
        // 저장된 원본 점을 걷기 중 지도와 같은 방식으로 다듬어서 표시
        val raw = db.walkDao().points(sessionId).map { RawPoint(it.time, it.lat, it.lon, it.accuracy, it.altitude) }
        value = smoothRoute(raw).map { GeoPoint(it.lat, it.lon) }
    }
    var confirmDelete by remember { mutableStateOf(false) }
    val s = session
    if (s == null) {
        Text("기록을 불러오는 중이거나 이미 삭제되었습니다.", Modifier.padding(16.dp))
        return
    }
    val fmt = NumberFormat.getInstance(Locale.KOREA)
    val start = Instant.ofEpochMilli(s.startTime).atZone(zone)
    val end = Instant.ofEpochMilli(s.endTime ?: s.startTime).atZone(zone)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RouteMap(points, Modifier.fillMaxWidth().height(320.dp))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(start.format(dayFmt), style = MaterialTheme.typography.titleMedium)
            Text("${timeFmt.format(start)} ~ ${timeFmt.format(end)}", style = MaterialTheme.typography.bodyMedium)
            Text(formatKm(s.distanceM), style = MaterialTheme.typography.displaySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Info("활동 시간", formatElapsed(s.activeMs))
                Info("평균 페이스", formatPace(s.distanceM, s.activeMs))
                Info("걸음", fmt.format(s.steps))
            }
            OutlinedButton(onClick = { confirmDelete = true }, Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                Text("기록 삭제")
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("이 기록을 삭제할까요?") },
            text = { Text("경로와 거리 기록이 모두 지워지고 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        db.walkDao().deleteSession(s.id) // 경로 점은 CASCADE로 함께 삭제
                        WalkService.updateDailyDistance(db, s.startTime)
                        onDeleted()
                    }
                }) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun Info(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

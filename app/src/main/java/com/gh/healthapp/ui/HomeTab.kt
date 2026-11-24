package com.gh.healthapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gh.healthapp.Section
import com.gh.healthapp.StepsUiState
import com.gh.healthapp.formatDuration
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 오늘 탭: 2×2 요약 타일(누르면 해당 탭으로 이동) + 최근 7일 평균 걸음 */
@Composable
fun HomeTab(state: StepsUiState.Loaded, onOpenTab: (Int) -> Unit) {
    val fmt = NumberFormat.getInstance(Locale.KOREA)
    val steps = (state.steps as? Section.Ok)?.data.orEmpty()
    val hr = (state.heartRate as? Section.Ok)?.data
    val sleep = (state.sleep as? Section.Ok)?.data
    val weight = (state.weight as? Section.Ok)?.data

    val todaySteps = steps.firstOrNull()?.second
    val yesterdaySteps = steps.getOrNull(1)?.second
    val avg7 = if (steps.isEmpty()) null else steps.map { it.second }.average().toLong()
    val dateFmt = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile("걸음", "${fmt.format(todaySteps ?: 0L)} 걸음", "어제 ${fmt.format(yesterdaySteps ?: 0L)}", { onOpenTab(1) }, Modifier.weight(1f))
            Tile(
                "심박",
                if (hr?.avg != null) "평균 ${hr.avg} bpm" else "기록 없음",
                if (hr?.latestBpm != null) "최근 ${hr.latestBpm}" else "오늘 기록 없음",
                { onOpenTab(1) },
                Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val day = sleep?.day
            Tile(
                "수면",
                if (day != null) formatDuration(day.asleep) else "기록 없음",
                when {
                    day == null -> "수면 기록 없음"
                    day.date == java.time.LocalDate.now() -> "오늘 수면"
                    else -> "어제 수면"
                },
                { onOpenTab(3) },
                Modifier.weight(1f),
            )
            val w = weight?.latest
            Tile(
                "체중",
                if (w != null) String.format(Locale.KOREA, "%.1f kg", w.kg) else "기록 없음",
                if (w != null) w.date.format(dateFmt) else "측정 없음",
                { onOpenTab(4) },
                Modifier.weight(1f),
            )
        }
        Text(
            "최근 7일 평균 걸음 ${avg7?.let { fmt.format(it) } ?: "-"}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun Tile(title: String, main: String, sub: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(main, style = MaterialTheme.typography.titleLarge)
            Text(sub, style = MaterialTheme.typography.bodySmall)
        }
    }
}

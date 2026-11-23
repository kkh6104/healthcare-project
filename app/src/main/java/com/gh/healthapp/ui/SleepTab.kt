package com.gh.healthapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.gh.healthapp.SleepNight
import com.gh.healthapp.StepsUiState
import com.gh.healthapp.ui.charts.BarChart
import com.gh.healthapp.ui.charts.BarItem
import com.gh.healthapp.ui.charts.Segment
import com.gh.healthapp.ui.charts.SleepColors
import com.gh.healthapp.ui.charts.StackedBar
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private fun fmtDuration(d: Duration): String {
    val m = d.toMinutes()
    return "${m / 60}시간 ${m % 60}분"
}

/** 수면 탭: 어젯밤 카드(단계 비율) + 최근 7일 막대 그래프 */
@Composable
fun SleepTab(state: StepsUiState.Loaded) {
    val sleep = (state.sleep as? Section.Ok)?.data
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (sleep == null) return@Column
        val hasAny = sleep.lastNight != null || sleep.recent.any { !it.second.isZero }
        if (!hasAny) {
            Text("수면 기록이 없습니다. 설정에서 삼성헬스 데이터를 가져올 수 있어요.", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        sleep.lastNight?.let { LastNightCard(it) }
        val ordered = sleep.recent.sortedBy { it.first }
        val nonZero = ordered.filter { !it.second.isZero }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("최근 7일", style = MaterialTheme.typography.titleMedium)
                BarChart(
                    items = ordered.mapIndexed { i, (date, d) ->
                        BarItem(
                            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                            d.toMinutes() / 60f,
                            highlight = i == ordered.lastIndex,
                        )
                    },
                    valueFormatter = { String.format(Locale.KOREAN, "%.1fh", it) },
                    goal = 7f,
                )
                if (nonZero.isNotEmpty()) {
                    val avg = Duration.ofMinutes(nonZero.sumOf { it.second.toMinutes() } / nonZero.size)
                    Text("평균 ${fmtDuration(avg)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun LastNightCard(n: SleepNight) {
    val timeFmt = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN).withZone(ZoneId.systemDefault())
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("어젯밤", style = MaterialTheme.typography.titleMedium)
            Text(fmtDuration(n.asleep), style = MaterialTheme.typography.displaySmall)
            Text("${timeFmt.format(n.start)} ~ ${timeFmt.format(n.end)}", style = MaterialTheme.typography.bodySmall)
            if (n.deep == null && n.light == null && n.rem == null && n.awake == null) {
                Text("단계 정보 없음", style = MaterialTheme.typography.bodyMedium)
            } else {
                fun min(d: Duration?) = (d?.toMinutes() ?: 0L).toFloat()
                StackedBar(
                    listOf(
                        Segment("깊은", min(n.deep), SleepColors.deep()),
                        Segment("얕은", min(n.light), SleepColors.light()),
                        Segment("렘", min(n.rem), SleepColors.rem()),
                        Segment("깸", min(n.awake), SleepColors.awake()),
                    )
                )
            }
        }
    }
}

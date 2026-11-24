package com.gh.healthapp.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gh.healthapp.SleepDay
import com.gh.healthapp.SleepNight
import com.gh.healthapp.data.DetailReader
import com.gh.healthapp.formatDuration
import com.gh.healthapp.ui.charts.BarChart
import com.gh.healthapp.ui.charts.BarItem
import com.gh.healthapp.ui.charts.Segment
import com.gh.healthapp.ui.charts.SleepColors
import com.gh.healthapp.ui.charts.StackedBar
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN).withZone(ZoneId.systemDefault())

/** 수면 상세: 날짜 ◀ ▶ 이동, 그날 합산 시간·단계 + 세션 목록 */
@Composable
fun SleepDayDetail(detail: DetailReader, initialDate: LocalDate?) {
    val todayEpoch = LocalDate.now().toEpochDay()
    var epochDay by rememberSaveable { mutableLongStateOf(initialDate?.toEpochDay() ?: todayEpoch) }
    val oldest = todayEpoch - 89
    val res by rememberDetailData(Unit) { detail.sleepDays(90) }
    val date = LocalDate.ofEpochDay(epochDay)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { epochDay -= 1 }, enabled = epochDay > oldest) { Text("◀") }
            Text(
                if (epochDay == todayEpoch) "오늘 (${date.format(detailDateFmt)})" else date.format(detailDateFmt),
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = { epochDay += 1 }, enabled = epochDay < todayEpoch) { Text("▶") }
        }
        DetailBody(res, isEmpty = { false }) { days ->
            val day = days.firstOrNull { it.date == date }
            if (day == null) {
                Text("이 날 수면 기록이 없습니다.", style = MaterialTheme.typography.bodyMedium)
            } else {
                DayContent(day)
            }
        }
    }
}

@Composable
private fun DayContent(day: SleepDay) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(formatDuration(day.asleep), style = MaterialTheme.typography.displaySmall)
            val range = "${timeFmt.format(day.firstStart)} ~ ${timeFmt.format(day.lastEnd)}"
            Text(
                if (day.sessions.size >= 2) "${day.sessions.size}회 나눠 잠 · $range" else range,
                style = MaterialTheme.typography.bodySmall,
            )
            val stages = listOf(
                Triple("깊은 수면", day.deep, SleepColors.deep()),
                Triple("얕은 수면", day.light, SleepColors.light()),
                Triple("렘 수면", day.rem, SleepColors.rem()),
                Triple("깸", day.awake, SleepColors.awake()),
            )
            if (stages.all { it.second == null }) {
                Text("단계 정보 없음", style = MaterialTheme.typography.bodyMedium)
            } else {
                StackedBar(stages.map { Segment(it.first, (it.second?.toMinutes() ?: 0L).toFloat(), it.third) })
                val total = stages.sumOf { it.second?.toMinutes() ?: 0L }.coerceAtLeast(1L)
                stages.forEach { (name, d, _) ->
                    val m = d?.toMinutes() ?: 0L
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name)
                        Text("${formatDuration(Duration.ofMinutes(m))} (${m * 100 / total}%)")
                    }
                }
            }
        }
    }
    Text("수면 세션 ${day.sessions.size}건", style = MaterialTheme.typography.titleMedium)
    day.sessions.forEach { n ->
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${timeFmt.format(n.start)} ~ ${timeFmt.format(n.end)}")
                Text(formatDuration(n.asleep))
            }
            stageSummary(n)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            HorizontalDivider(Modifier.padding(top = 6.dp))
        }
    }
}

/** 수면 기록 상세: 기간 막대 그래프(시간) + 하루 합산 기준 평균/최장/최단 + 날짜별 목록 */
@Composable
fun SleepHistoryDetail(detail: DetailReader) {
    var days by rememberSaveable { mutableIntStateOf(30) }
    val res by rememberDetailData(days) { detail.sleepDays(days) }
    Column(Modifier.fillMaxSize()) {
        PeriodChips(days, { days = it }, Modifier.padding(16.dp))
        DetailBody(res, isEmpty = { it.isEmpty() }) { sleepDays ->
            val today = LocalDate.now()
            val byDate = sleepDays.associate { it.date to it.asleep }
            val asc = (days - 1 downTo 0).map { i -> today.minusDays(i.toLong()).let { it to (byDate[it] ?: Duration.ZERO) } }
            val longest = sleepDays.maxByOrNull { it.asleep }!!
            val shortest = sleepDays.minByOrNull { it.asleep }!!
            val avg = Duration.ofMinutes(sleepDays.sumOf { it.asleep.toMinutes() } / sleepDays.size)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BarChart(
                                items = asc.mapIndexed { i, (d, dur) ->
                                    BarItem(barLabel(d, i, asc.size, days), dur.toMinutes() / 60f, highlight = i == asc.lastIndex)
                                },
                                height = 200.dp,
                                valueFormatter = { String.format(Locale.KOREAN, "%.1fh", it) },
                                goal = 7f,
                            )
                            Text("평균 ${formatDuration(avg)} (기록 있는 ${sleepDays.size}일)", style = MaterialTheme.typography.bodyMedium)
                            Text("가장 길었던 날 ${longest.date.format(detailDateFmt)} ${formatDuration(longest.asleep)}", style = MaterialTheme.typography.bodyMedium)
                            Text("가장 짧았던 날 ${shortest.date.format(detailDateFmt)} ${formatDuration(shortest.asleep)}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                items(sleepDays) { d ->
                    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(d.date.format(detailDateFmt))
                            Text(formatDuration(d.asleep))
                        }
                        val range = "${timeFmt.format(d.firstStart)} ~ ${timeFmt.format(d.lastEnd)}"
                        Text(
                            if (d.sessions.size >= 2) "${d.sessions.size}회 · $range" else range,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        stageSummary(d.deep, d.light, d.rem, d.awake)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        HorizontalDivider(Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}

private fun stageSummary(n: SleepNight): String? = stageSummary(n.deep, n.light, n.rem, n.awake)

private fun stageSummary(deep: Duration?, light: Duration?, rem: Duration?, awake: Duration?): String? {
    if (deep == null && light == null && rem == null && awake == null) return null
    fun s(label: String, d: Duration?) = "${label} ${formatDuration(d ?: Duration.ZERO)}"
    return listOf(s("깊은", deep), s("얕은", light), s("렘", rem), s("깸", awake)).joinToString(" · ")
}

package com.gh.healthapp.ui.detail

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
import com.gh.healthapp.data.DetailReader
import com.gh.healthapp.ui.charts.LineChart
import com.gh.healthapp.ui.charts.LinePoint
import java.time.LocalDate

/** 심박 상세: 날짜별 시간대 그래프 + 기간별 일평균 그래프 */
@Composable
fun HeartRateDetail(detail: DetailReader) {
    val todayEpoch = LocalDate.now().toEpochDay()
    var epochDay by rememberSaveable { mutableLongStateOf(todayEpoch) }
    var days by rememberSaveable { mutableIntStateOf(30) }
    val hourlyRes by rememberDetailData(epochDay) { detail.heartRateHourly(LocalDate.ofEpochDay(epochDay)) }
    val dailyRes by rememberDetailData(days) { detail.hrDailyAvg(days) }
    val date = LocalDate.ofEpochDay(epochDay)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { epochDay -= 1 }) { Text("◀") }
            Text(
                if (epochDay == todayEpoch) "오늘 (${date.format(detailDateFmt)})" else date.format(detailDateFmt),
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = { epochDay += 1 }, enabled = epochDay < todayEpoch) { Text("▶") }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("시간대별 심박수", style = MaterialTheme.typography.titleMedium)
                DetailBody(hourlyRes, isEmpty = { it.isEmpty() }, emptyText = "이 날 심박수 기록이 없습니다.") { hours ->
                    LineChart(
                        points = hours.map { (h, avg, _) -> LinePoint(h.toFloat(), avg.toFloat(), "${h}시") },
                        yFormatter = { it.toInt().toString() },
                        minYSpan = 30f,
                    )
                    val min = hours.minOf { it.third.first }
                    val max = hours.maxOf { it.third.second }
                    val avg = hours.map { it.second }.average()
                    Text(
                        "평균 ${avg.toInt()} · 최저 ${min.toInt()} · 최고 ${max.toInt()} bpm",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        PeriodChips(days, { days = it })
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("일별 평균 심박수", style = MaterialTheme.typography.titleMedium)
                DetailBody(dailyRes, isEmpty = { list -> list.all { it.second == null } }) { list ->
                    val pts = list.sortedBy { it.first }.mapNotNull { (d, v) ->
                        v?.let { LinePoint(d.toEpochDay().toFloat(), it.toFloat(), d.format(detailShortDate)) }
                    }
                    LineChart(points = pts, yFormatter = { it.toInt().toString() }, minYSpan = 20f)
                    val values = list.mapNotNull { it.second }
                    Text(
                        "기간 평균 ${values.average().toInt()} · 최저 ${values.min().toInt()} · 최고 ${values.max().toInt()} bpm (일 평균 기준)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

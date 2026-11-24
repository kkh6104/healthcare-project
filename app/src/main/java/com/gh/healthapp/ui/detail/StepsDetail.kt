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
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gh.healthapp.data.DetailReader
import com.gh.healthapp.ui.charts.BarChart
import com.gh.healthapp.ui.charts.BarItem
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val GOAL = 6000L

/** 걸음 상세: 기간 막대 그래프 + 평균/최고/목표 달성일 + 날짜별 목록 */
@Composable
fun StepsDetail(detail: DetailReader) {
    var days by rememberSaveable { mutableIntStateOf(30) }
    val res by rememberDetailData(days) { detail.stepsDaily(days) }
    val fmt = NumberFormat.getInstance(Locale.KOREA)
    Column(Modifier.fillMaxSize()) {
        PeriodChips(days, { days = it }, Modifier.padding(16.dp))
        DetailBody(res, isEmpty = { list -> list.all { it.second == 0L } }) { list ->
            val asc = list.sortedBy { it.first }
            val avg = asc.sumOf { it.second } / asc.size
            val best = asc.maxByOrNull { it.second }!!
            val goalDays = asc.count { it.second >= GOAL }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BarChart(
                                items = asc.mapIndexed { i, (d, v) ->
                                    BarItem(barLabel(d, i, asc.size, days), v.toFloat(), highlight = i == asc.lastIndex)
                                },
                                height = 200.dp,
                                valueFormatter = { fmt.format(it.toLong()) },
                                goal = GOAL.toFloat(),
                            )
                            Text("평균 ${fmt.format(avg)}걸음 · 최고 ${fmt.format(best.second)}걸음(${best.first.format(detailDateFmt)})", style = MaterialTheme.typography.bodyMedium)
                            Text("목표 ${fmt.format(GOAL)}걸음 달성 ${goalDays}일 / ${asc.size}일", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                items(list) { (date, steps) ->
                    Column {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(date.format(detailDateFmt))
                            Text("${fmt.format(steps)} 걸음")
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/** 7일은 요일, 30/90일은 듬성듬성 "11/1" 라벨 */
internal fun barLabel(date: LocalDate, index: Int, size: Int, days: Int): String {
    if (days <= 7) return date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
    val step = if (days <= 30) 7 else 15
    return if ((size - 1 - index) % step == 0) date.format(detailShortDate) else ""
}

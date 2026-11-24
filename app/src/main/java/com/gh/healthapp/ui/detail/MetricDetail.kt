package com.gh.healthapp.ui.detail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gh.healthapp.BodyMetric
import com.gh.healthapp.data.DetailReader
import com.gh.healthapp.data.RecordType
import com.gh.healthapp.ui.charts.LineChart
import com.gh.healthapp.ui.charts.LinePoint
import java.time.ZoneId
import java.util.Locale

/** 체중 상세 */
@Composable
fun WeightDetail(detail: DetailReader) {
    MetricHistory(detail, RecordType.WEIGHT, "kg", minYSpan = 6f)
}

/** 체성분 상세: 지표 칩 + 선택한 지표의 기록 */
@Composable
fun BodyCompositionDetail(detail: DetailReader) {
    var metricName by rememberSaveable { mutableStateOf(BodyMetric.BODY_FAT.name) }
    val metric = BodyMetric.valueOf(metricName)
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BodyMetric.entries.forEach { m ->
                FilterChip(selected = m == metric, onClick = { metricName = m.name }, label = { Text(m.label) })
            }
        }
        val type = when (metric) {
            BodyMetric.BODY_FAT -> RecordType.BODY_FAT
            BodyMetric.BONE_MASS -> RecordType.BONE_MASS
            BodyMetric.LEAN_BODY_MASS -> RecordType.LEAN_BODY_MASS
            BodyMetric.BODY_WATER_MASS -> RecordType.BODY_WATER_MASS
        }
        val span = when (metric) {
            BodyMetric.BODY_FAT -> 10f
            BodyMetric.BONE_MASS -> 1f
            else -> 6f
        }
        MetricHistory(detail, type, metric.unit, minYSpan = span)
    }
}

/** 기간 선택 + LineChart + 최저/최고/평균/변화 + 측정 목록(최신순) */
@Composable
private fun MetricHistory(detail: DetailReader, type: String, unit: String, minYSpan: Float?) {
    var days by rememberSaveable(type) { mutableIntStateOf(30) }
    val res by rememberDetailData(type to days) { detail.metricHistory(type, days) }
    val zone = ZoneId.systemDefault()
    Column(Modifier.fillMaxSize()) {
        PeriodChips(days, { days = it }, Modifier.padding(16.dp))
        DetailBody(res, isEmpty = { it.isEmpty() }) { list ->
            val values = list.map { it.second }
            val change = values.last() - values.first()
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LineChart(
                                points = list.map { (t, v) ->
                                    val d = t.atZone(zone).toLocalDate()
                                    LinePoint(d.toEpochDay().toFloat(), v.toFloat(), d.format(detailShortDate))
                                },
                                yFormatter = { f1(it.toDouble()) },
                                minYSpan = minYSpan,
                            )
                            Text(
                                "최저 ${f1(values.min())} · 최고 ${f1(values.max())} · 평균 ${f1(values.average())} ${unit}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                if (list.size < 2) "기간 내 변화: 측정이 1건뿐입니다" else "기간 내 변화 ${signed(change)} ${unit}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                val desc = list.reversed()
                itemsIndexed(desc) { i, (t, v) ->
                    val prev = desc.getOrNull(i + 1)?.second
                    Column {
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(t.atZone(zone).toLocalDate().format(detailDateFmt))
                            Text(
                                "${f1(v)} ${unit}" + (prev?.let { "   (${signed(v - it)})" } ?: ""),
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

private fun signed(v: Double): String {
    val zero = f1(v).trimStart('-', '+').all { it == '0' || it == '.' }
    return if (zero) "변화 없음" else String.format(Locale.KOREAN, "%+.1f", v)
}

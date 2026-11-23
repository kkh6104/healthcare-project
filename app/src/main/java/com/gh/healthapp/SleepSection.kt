package com.gh.healthapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Duration → "N시간 M분" (0시간이면 "M분") */
fun formatDuration(d: Duration): String {
    val h = d.toHours()
    val m = d.toMinutes() % 60
    return if (h == 0L) "${m}분" else "${h}시간 ${m}분"
}

@Composable
fun SleepCard(summary: SleepSummary, modifier: Modifier = Modifier) {
    val zone = ZoneId.systemDefault()
    val timeFmt = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN).withZone(zone)
    val dateFmt = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("수면", style = MaterialTheme.typography.titleMedium)
            val n = summary.lastNight
            if (n == null) {
                Text("최근 수면 기록이 없습니다.")
            } else {
                Text(formatDuration(n.asleep), style = MaterialTheme.typography.displaySmall)
                Text(
                    "${timeFmt.format(n.start)} ~ ${timeFmt.format(n.end)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (n.deep != null && n.light != null && n.rem != null && n.awake != null) {
                    Text(
                        "깊은 수면 ${formatDuration(n.deep)} · 얕은 수면 ${formatDuration(n.light)} · " +
                            "렘 ${formatDuration(n.rem)} · 깸 ${formatDuration(n.awake)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Column(Modifier.padding(top = 12.dp)) {
                summary.recent.forEach { (date, dur) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(date.format(dateFmt))
                        Text(if (dur.isZero) "-" else formatDuration(dur))
                    }
                }
            }
        }
    }
}

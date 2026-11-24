package com.gh.healthapp.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class DetailKind(val title: String) {
    STEPS("걸음"),
    HEART_RATE("심박수"),
    SLEEP_LAST_NIGHT("수면 상세"),
    SLEEP_HISTORY("수면 기록"),
    WEIGHT("체중"),
    BODY_COMPOSITION("체성분"),
}

internal val detailDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
internal val detailShortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d", Locale.KOREAN)

internal fun f1(v: Double): String = String.format(Locale.KOREAN, "%.1f", v)

/** 카드 제목 줄: 왼쪽 제목, 오른쪽 위 작은 "자세히 ›" */
@Composable
fun DetailHeader(title: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text("자세히 ›", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
}

/** 7일 / 30일 / 90일 선택 칩 */
@Composable
fun PeriodChips(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(7, 30, 90).forEach { d ->
            FilterChip(selected = d == selected, onClick = { onSelect(d) }, label = { Text("${d}일") })
        }
    }
}

/** 읽기 결과. data가 null이면 읽기 실패 */
class Loadable<T>(val data: T?)

/** DB 읽기 (key가 바뀌면 다시 읽음). 아직 읽는 중이면 null */
@Composable
fun <T> rememberDetailData(key: Any?, block: suspend () -> T): State<Loadable<T>?> =
    produceState<Loadable<T>?>(initialValue = null, key1 = key) {
        value = null
        value = Loadable(
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        )
    }

/** 읽는 중 / 실패 / 기록 없음 / 내용을 구분해서 보여 준다 */
@Composable
fun <T> DetailBody(
    res: Loadable<T>?,
    isEmpty: (T) -> Boolean,
    emptyText: String = "기록이 없습니다.",
    content: @Composable (T) -> Unit,
) {
    val data = res?.data
    when {
        res == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        data == null -> Text("불러오지 못했습니다.", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
        isEmpty(data) -> Text(emptyText, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
        else -> content(data)
    }
}

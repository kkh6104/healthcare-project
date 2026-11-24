package com.gh.healthapp.ui.detail

import androidx.compose.runtime.Composable
import com.gh.healthapp.Section
import com.gh.healthapp.StepsUiState
import com.gh.healthapp.data.DetailReader

/** 카드를 눌렀을 때 보이는 상세 화면 (DB 읽기만 사용) */
@Composable
fun DetailScreen(kind: DetailKind, state: StepsUiState.Loaded, detail: DetailReader) {
    when (kind) {
        DetailKind.STEPS -> StepsDetail(detail)
        DetailKind.HEART_RATE -> HeartRateDetail(detail)
        DetailKind.SLEEP_LAST_NIGHT -> SleepDayDetail(detail, (state.sleep as? Section.Ok)?.data?.day?.date)
        DetailKind.SLEEP_HISTORY -> SleepHistoryDetail(detail)
        DetailKind.WEIGHT -> WeightDetail(detail)
        DetailKind.BODY_COMPOSITION -> BodyCompositionDetail(detail)
    }
}

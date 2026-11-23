package com.gh.healthapp.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gh.healthapp.PrivacyPolicyActivity
import com.gh.healthapp.StepsDiagnostics
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 설정 화면: 새로고침 / 삼성헬스 가져오기 / 걸음 기록 진단 / 동기화 상태 / 개인정보 안내 */
@Composable
fun SettingsScreen(
    lastSync: Long?,
    syncing: Boolean,
    importProgress: String?,
    diagnostics: StepsDiagnostics?,
    onRefresh: () -> Unit,
    onImport: () -> Unit,
    onDiagnose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val timeFmt = DateTimeFormatter.ofPattern("M월 d일 a h:mm", Locale.KOREAN).withZone(ZoneId.systemDefault())
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("동기화", style = MaterialTheme.typography.titleMedium)
        Text(
            when {
                syncing -> "동기화 중…"
                lastSync != null -> "마지막 동기화: ${timeFmt.format(Instant.ofEpochMilli(lastSync))}"
                else -> "아직 동기화하지 않았습니다"
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = onRefresh, enabled = importProgress == null) { Text("새로고침") }

        HorizontalDivider()
        Text("삼성헬스 데이터", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = onImport, enabled = importProgress == null) { Text("삼성헬스 데이터 가져오기") }
        Text(
            importProgress ?: "Download/Samsung Health 안의 samsunghealth_... 폴더를 선택하세요",
            style = MaterialTheme.typography.bodySmall,
        )

        HorizontalDivider()
        Text("진단", style = MaterialTheme.typography.titleMedium)
        if (diagnostics == null) {
            OutlinedButton(onClick = onDiagnose) { Text("걸음 기록 진단") }
        } else {
            Text(
                if (diagnostics.recordCount == 0) "Health Connect 걸음 기록: 0건 (삼성헬스가 아직 걸음 수를 넘기지 않음)"
                else "Health Connect 걸음 기록: ${diagnostics.recordCount}건 · 출처: ${diagnostics.sources.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        HorizontalDivider()
        OutlinedButton(onClick = { context.startActivity(Intent(context, PrivacyPolicyActivity::class.java)) }) {
            Text("개인정보 처리 안내")
        }
    }
}

package com.gh.healthapp.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gh.healthapp.PrivacyPolicyActivity
import com.gh.healthapp.StepsDiagnostics
import com.gh.healthapp.chat.ChatConsent
import com.gh.healthapp.chat.KnowledgeBase
import com.gh.healthapp.chat.KbStatus
import kotlinx.coroutines.launch
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
    onRequestAiConsent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var aiConsent by remember { mutableStateOf(ChatConsent.isGiven(context)) }
    var kbStatus by remember { mutableStateOf<KbStatus?>(null) }
    var kbError by remember { mutableStateOf<String?>(null) }
    var preparing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(context) { kbStatus = KnowledgeBase.status(context) }
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
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("AI 데이터 전송 동의", Modifier.weight(1f))
            Switch(checked = aiConsent, onCheckedChange = { given ->
                if (given) onRequestAiConsent() else {
                    ChatConsent.set(context, false)
                    aiConsent = false
                }
            })
        }
        HorizontalDivider()
        Text("건강 지식 데이터", style = MaterialTheme.typography.titleMedium)
        Text(when {
            preparing -> "준비 중…"
            kbError != null || kbStatus?.lastError != null -> "오류: ${kbError ?: kbStatus?.lastError}"
            kbStatus?.indexed == true -> "색인된 조각 ${kbStatus?.chunks}개"
            else -> "아직 준비 안 됨"
        })
        OutlinedButton(enabled = !preparing, onClick = {
            preparing = true
            kbError = null
            scope.launch {
                val result = KnowledgeBase.ensureIndexed(context)
                kbStatus = KnowledgeBase.status(context)
                kbError = result.exceptionOrNull()?.message ?: if (result.isFailure) "준비하지 못했습니다" else null
                preparing = false
            }
        }) { Text("다시 준비") }
        HorizontalDivider()
        OutlinedButton(onClick = { context.startActivity(Intent(context, PrivacyPolicyActivity::class.java)) }) {
            Text("개인정보 처리 안내")
        }
    }
}

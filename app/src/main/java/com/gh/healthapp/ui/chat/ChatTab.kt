package com.gh.healthapp.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gh.healthapp.chat.ChatConsent
import com.gh.healthapp.chat.ChatError
import com.gh.healthapp.chat.ChatException
import com.gh.healthapp.chat.ChatRepository
import com.gh.healthapp.chat.KbSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTab() {
    val context = LocalContext.current
    var consent by remember { mutableStateOf(ChatConsent.isGiven(context)) }
    if (!consent) {
        ConsentScreen {
            ChatConsent.set(context, true)
            consent = true
        }
        return
    }
    val repository = remember(context) { ChatRepository(context) }
    val messages by repository.messages.collectAsState(initial = emptyList())
    val progress by repository.progress.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ChatError?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var selectedSource by remember { mutableStateOf<KbSource?>(null) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)

    LaunchedEffect(progress?.startedAt) {
        val started = progress?.startedAt
        if (started != null) {
            elapsedSeconds = 0
            while (true) {
                elapsedSeconds = ((System.currentTimeMillis() - started).coerceAtLeast(0) / 1000).toInt()
                delay(1000)
            }
        }
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || sending) return
        input = ""
        error = null
        sending = true
        scope.launch {
            val result = repository.send(trimmed)
            error = result.exceptionOrNull()?.let { (it as? ChatException)?.error ?: ChatError.Other(it.message ?: "오류가 발생했습니다.") }
            sending = false
        }
    }
    fun resend(id: Long) {
        if (sending) return
        error = null
        sending = true
        scope.launch {
            val result = repository.resend(id)
            error = result.exceptionOrNull()?.let { (it as? ChatException)?.error ?: ChatError.Other(it.message ?: "오류가 발생했습니다.") }
            sending = false
        }
    }
    LaunchedEffect(messages.size, sending, error, imeBottom, progress) {
        val count = messages.size + (if (sending || progress != null || error != null) 1 else 0)
        if (count > 0) listState.animateScrollToItem(count - 1)
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("AI 건강 도우미", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Box {
                TextButton(onClick = { menuOpen = true }) { Text("메뉴") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("대화 지우기") }, onClick = { menuOpen = false; confirmClear = true }, enabled = !sending)
                }
            }
        }
        if (!repository.hasApiKey()) {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text("API 키가 설정되지 않았습니다 (local.properties의 GEMINI_API_KEY)", Modifier.padding(16.dp))
            }
        }
        if (messages.isEmpty() && !sending && error == null) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text("추천 질문", style = MaterialTheme.typography.titleSmall)
                listOf("이번 주 수면 어때?", "지난달이랑 걸음 비교해줘", "체중 변화 추세 알려줘", "요즘 건강 조언 해줘").forEach { question ->
                    SuggestionChip(onClick = { send(question) }, label = { Text(question) })
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(messages, key = { it.id }) { message ->
                    Column {
                        Bubble(if (message.role == "user") AnnotatedString(message.text) else formatAnswer(message.text), message.role == "user", copyText = message.text)
                        if (message.role != "user" && message.sources.isNotEmpty()) {
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                message.sources.forEach { source ->
                                    SuggestionChip(onClick = { selectedSource = source }, label = { Text("근거 [${source.index}] ${source.title}") })
                                }
                            }
                        }
                    }
                }
                if (sending || progress != null) item {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth(0.85f)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            val current = progress
                            Text(if (current != null && (current.retrying || current.attempt >= 2))
                                "서버가 붐벼서 다시 시도하는 중… (${current.attempt}/${current.maxAttempts})"
                            else "생각하는 중… ${if (current == null) 0 else elapsedSeconds}초")
                        }
                    }
                }
                else error?.let { current -> item {
                    Column {
                        Bubble(AnnotatedString(errorText(current)), false, isError = true)
                        messages.lastOrNull()?.takeIf { it.role == "user" }?.let { message ->
                            TextButton(onClick = { resend(message.id) }, enabled = !sending) { Text("다시 보내기") }
                        }
                    }
                } }
            }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), label = { Text("메시지") }, enabled = !sending)
            Button(onClick = { send(input) }, enabled = input.isNotBlank() && !sending) { Text("전송") }
        }
    }
    if (confirmClear) {
        AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text("대화를 지울까요?") }, text = { Text("저장된 대화가 삭제됩니다.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; scope.launch { repository.clear(); error = null } }) { Text("지우기") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("취소") } })
    }
    selectedSource?.let { source ->
        val uriHandler = LocalUriHandler.current
        ModalBottomSheet(onDismissRequest = { selectedSource = null }) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(source.title, style = MaterialTheme.typography.titleLarge)
                Text(source.heading, style = MaterialTheme.typography.titleMedium)
                Text(source.source, style = MaterialTheme.typography.bodyMedium)
                source.sourceUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    TextButton(onClick = { uriHandler.openUri(url) }) { Text(url) }
                }
                Text(source.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ConsentScreen(onConsent: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("AI 데이터 전송 동의", style = MaterialTheme.typography.headlineSmall)
        Text("보내는 것: 날짜별 걸음, 수면, 심박, 체중, 체성분 요약 숫자와 걷기 거리·시간")
        Text("보내지 않는 것: 위치·경로, 이름, 기기 정보")
        Text("Gemini 무료 등급에서는 Google이 입력 내용을 서비스 개선에 사용할 수 있습니다.")
        Button(onClick = onConsent) { Text("동의하고 시작") }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Bubble(text: AnnotatedString, user: Boolean, isError: Boolean = false, copyText: String? = text.text) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
        Box(Modifier.fillMaxWidth(0.85f)) {
            Surface(shape = MaterialTheme.shapes.medium, color = when { isError -> MaterialTheme.colorScheme.errorContainer; user -> MaterialTheme.colorScheme.primaryContainer; else -> MaterialTheme.colorScheme.surfaceVariant }, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { if (copyText != null) menuOpen = true })) {
                Text(text, Modifier.padding(12.dp), color = when { isError -> MaterialTheme.colorScheme.onErrorContainer; user -> MaterialTheme.colorScheme.onPrimaryContainer; else -> MaterialTheme.colorScheme.onSurfaceVariant })
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("복사") }, onClick = {
                    clipboard.setText(AnnotatedString(copyText ?: text.text))
                    menuOpen = false
                    Toast.makeText(context, "복사했어요", Toast.LENGTH_SHORT).show()
                })
            }
        }
    }
}

private fun errorText(error: ChatError): String = when (error) {
    ChatError.NoKey -> "API 키가 설정되지 않았습니다."
    ChatError.Network -> "네트워크 연결을 확인해 주세요."
    ChatError.RateLimited -> "요청이 많아요. 잠시 후 다시 시도해 주세요"
    ChatError.Busy -> "AI 서버가 붐벼요. 잠시 후 다시 시도해 주세요"
    is ChatError.Other -> error.message
}

private fun formatAnswer(text: String): AnnotatedString = buildAnnotatedString {
    text.lines().forEachIndexed { index, line ->
        if (index > 0) append('\n')
        val content = if (line.startsWith("- ")) "• " + line.drop(2) else line
        var position = 0
        while (position < content.length) {
            val start = content.indexOf("**", position)
            if (start < 0) { append(content.substring(position)); break }
            val end = content.indexOf("**", start + 2)
            if (end < 0) { append(content.substring(position)); break }
            append(content.substring(position, start))
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(content.substring(start + 2, end))
            pop()
            position = end + 2
        }
    }
}

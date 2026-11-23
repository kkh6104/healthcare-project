package com.gh.healthapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gh.healthapp.ui.ActivityTab
import com.gh.healthapp.ui.BodyTab
import com.gh.healthapp.ui.HomeTab
import com.gh.healthapp.ui.SettingsScreen
import com.gh.healthapp.ui.SleepTab
import com.gh.healthapp.ui.icons.HealthIcons
import com.gh.healthapp.ui.theme.HealthAppTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthAppTheme {
                StepsScreen()
            }
        }
    }
}

private data class TabItem(val label: String, val icon: ImageVector)

private val TABS = listOf(
    TabItem("오늘", Icons.Default.Home),
    TabItem("활동", HealthIcons.Walk),
    TabItem("수면", HealthIcons.Sleep),
    TabItem("몸", HealthIcons.Weight),
)

@Composable
fun StepsScreen(vm: StepsViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val diagState by vm.diagnostics.collectAsState()
    val importProgress by vm.importProgress.collectAsState()
    val lastSync by vm.lastSync.collectAsState(null)
    val syncing by vm.syncing.collectAsState()
    val context = LocalContext.current

    // 권한 요청 창. 결과가 오면 다시 불러오기
    val permissionLauncher = rememberLauncherForActivityResult(vm.manager.permissionContract()) {
        vm.refresh("permission")
    }
    // 삼성헬스 내보내기 폴더 선택
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.importSamsung(uri)
    }

    // 앱이 화면에 나올 때마다 새로고침 (설치/설정 후 돌아왔을 때 반영되도록)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) { vm.refreshIfStale() }
    }

    when (val s = state) {
        is StepsUiState.Loaded -> LoadedShell(
            s = s,
            lastSync = lastSync,
            syncing = syncing,
            importProgress = importProgress,
            diagnostics = diagState,
            onRefresh = { vm.refresh("button") },
            onImport = { folderLauncher.launch(null) },
            onDiagnose = { vm.loadDiagnostics() },
        )

        else -> Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("내 건강 데이터", style = MaterialTheme.typography.headlineMedium)
                when (s) {
                    StepsUiState.Loading -> CircularProgressIndicator()

                    is StepsUiState.Unavailable -> {
                        if (s.availability == HealthConnectManager.Availability.INSTALL_OR_UPDATE_REQUIRED) {
                            Text("Health Connect 설치 또는 업데이트가 필요합니다.")
                            Button(onClick = { vm.manager.openPlayStore(context) }) {
                                Text("플레이스토어에서 열기")
                            }
                        } else {
                            Text("이 기기에서는 Health Connect를 사용할 수 없습니다.")
                        }
                    }

                    StepsUiState.NeedPermission -> {
                        Text("걸음 수, 심박수, 수면, 몸무게, 체성분을 읽으려면 Health Connect 권한이 필요합니다.")
                        Button(onClick = { permissionLauncher.launch(HealthConnectManager.PERMISSIONS) }) {
                            Text("권한 허용")
                        }
                    }

                    is StepsUiState.Error -> {
                        Text("오류: ${s.message}", color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = { vm.refresh("retry") }) { Text("다시 시도") }
                    }

                    is StepsUiState.Loaded -> Unit
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadedShell(
    s: StepsUiState.Loaded,
    lastSync: Long?,
    syncing: Boolean,
    importProgress: String?,
    diagnostics: StepsDiagnostics?,
    onRefresh: () -> Unit,
    onImport: () -> Unit,
    onDiagnose: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    // 닫은 배너 문구 (같은 문구는 다시 안 보임, 새 문구가 오면 다시 표시)
    var dismissedBanner by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = showSettings) { showSettings = false }

    val syncTimeFmt = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN).withZone(ZoneId.systemDefault())
    val subtitle = when {
        syncing -> "동기화 중…"
        lastSync != null -> "마지막 동기화: ${syncTimeFmt.format(Instant.ofEpochMilli(lastSync))}"
        else -> "아직 동기화하지 않았습니다"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (showSettings) "설정" else "내 건강 데이터", style = MaterialTheme.typography.titleLarge)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    if (showSettings) {
                        IconButton(onClick = { showSettings = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                        }
                    }
                },
                actions = {
                    if (!showSettings) {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "설정")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!showSettings) {
                NavigationBar {
                    TABS.forEachIndexed { i, item ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            val banner = s.banner
            if (banner != null && banner != dismissedBanner) {
                BannerCard(banner, onDismiss = { dismissedBanner = banner })
            }
            if (showSettings) {
                SettingsScreen(
                    lastSync = lastSync,
                    syncing = syncing,
                    importProgress = importProgress,
                    diagnostics = diagnostics,
                    onRefresh = onRefresh,
                    onImport = onImport,
                    onDiagnose = onDiagnose,
                )
            } else {
                when (tab) {
                    0 -> HomeTab(s, onOpenTab = { tab = it })
                    1 -> ActivityTab(s)
                    2 -> SleepTab(s)
                    else -> BodyTab(s)
                }
            }
        }
    }
}

/** 성공("가져오기 완료")은 secondaryContainer, 오류/경고는 errorContainer */
@Composable
private fun BannerCard(text: String, onDismiss: () -> Unit) {
    val success = text.startsWith("가져오기 완료")
    val container = if (success) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
    val content = if (success) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Card(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Row(Modifier.padding(start = 16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f).padding(vertical = 12.dp), color = content)
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "닫기", tint = content) }
        }
    }
}

package com.gh.healthapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gh.healthapp.ui.theme.HealthAppTheme

/** Health Connect 권한 화면에서 "개인정보처리방침"을 누르면 열리는 화면 */
class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthAppTheme {
                Scaffold(Modifier.fillMaxSize()) { p ->
                    Column(Modifier.padding(p).padding(16.dp)) {
                        Text("개인정보 처리 안내", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "이 앱은 Health Connect의 걸음 수, 심박수, 수면, 몸무게, 체성분 데이터를 읽어 화면에 표시합니다. " +
                                "데이터는 기기 밖으로 전송되지 않습니다.",
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

package com.gh.healthapp.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 탭 아이콘 3종. material-icons-extended를 쓰지 않으려고 직접 정의했다.
 * 정확한 Google Material Icons(Apache License 2.0)의 path 데이터를 확신할 수 없어
 * "DirectionsWalk" / "Bedtime" / "MonitorWeight"와 비슷한 모양을 **직접 그렸다**(24dp 격자).
 * 색은 Icon의 tint가 덮어쓰므로 검정은 자리표시일 뿐이다.
 */
object HealthIcons {

    private fun builder(name: String) = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )

    private val ink = SolidColor(Color.Black)

    /** 걷는 사람 실루엣 (직접 그림) */
    val Walk: ImageVector by lazy {
        builder("HealthWalk")
            // 머리
            .path(fill = ink) {
                moveTo(13.5f, 3.2f)
                arcTo(1.9f, 1.9f, 0f, true, true, 13.5f, 7f)
                arcTo(1.9f, 1.9f, 0f, true, true, 13.5f, 3.2f)
                close()
            }
            // 몸통·팔·다리
            .path(
                stroke = ink,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(13f, 9f); lineTo(11f, 14f)          // 몸통
                moveTo(13f, 9.5f); lineTo(9f, 11.5f)       // 뒤로 뻗은 팔
                moveTo(13f, 9.5f); lineTo(16f, 12f)        // 앞으로 뻗은 팔
                moveTo(11f, 14f); lineTo(8f, 21f)          // 뒤쪽 다리
                moveTo(11f, 14f); lineTo(14f, 17f); lineTo(15f, 21f) // 앞쪽 다리
            }
            .build()
    }

    /** 초승달 (직접 그림) */
    val Sleep: ImageVector by lazy {
        builder("HealthSleep")
            .path(fill = ink) {
                moveTo(12f, 3f)
                arcTo(9f, 9f, 0f, true, false, 21f, 12f)   // 큰 원 (왼쪽으로 돌아 오른쪽 끝까지)
                arcTo(7f, 7f, 0f, true, true, 12f, 3f)     // 안쪽 원호로 베어 낸 초승달 모양
                close()
            }
            .build()
    }

    /** 체중계 (직접 그림): 둥근 사각형 + 눈금 반원 + 바늘 */
    val Weight: ImageVector by lazy {
        builder("HealthWeight")
            .path(
                stroke = ink,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // 몸체
                moveTo(7f, 4f); lineTo(17f, 4f)
                arcTo(3f, 3f, 0f, false, true, 20f, 7f)
                lineTo(20f, 17f)
                arcTo(3f, 3f, 0f, false, true, 17f, 20f)
                lineTo(7f, 20f)
                arcTo(3f, 3f, 0f, false, true, 4f, 17f)
                lineTo(4f, 7f)
                arcTo(3f, 3f, 0f, false, true, 7f, 4f)
                close()
                // 눈금 반원
                moveTo(8f, 11f)
                arcTo(4f, 4f, 0f, false, true, 16f, 11f)
                // 바늘
                moveTo(12f, 11f); lineTo(14f, 8.5f)
            }
            .build()
    }
}

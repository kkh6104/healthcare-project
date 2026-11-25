# 4단계 AI 챗봇 설계 (Gemini API)

## 결정사항
- Gemini API (Google AI Studio 무료 등급). REST 직접 호출: `POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`, 헤더 `x-goog-api-key`. **외부 SDK/HTTP 라이브러리 추가 없이** HttpURLConnection + org.json, Dispatchers.IO.
- 모델: 상수 `GeminiConfig.MODEL = "gemini-flash-latest"` (바꾸기 쉽게 한 곳에).
- API 키: 개인용 단계. `local.properties`의 `GEMINI_API_KEY` → BuildConfig.GEMINI_API_KEY (없으면 빈 문자열). **키를 로그·화면·파일에 절대 출력하지 않는다.** 키가 비면 채팅 화면에 "API 키가 설정되지 않았습니다 (local.properties의 GEMINI_API_KEY)" 안내.
- 함수 호출(function calling) 방식 RAG: 질문 + 최근 7일 요약을 보내고, 모델이 더 필요한 데이터를 도구로 요청 → 앱이 로컬 DB에서 꺼내 functionResponse로 응답 → 최종 답. 한 질문당 도구 왕복 최대 4회.
- 보내는 데이터: 날짜별 숫자 요약만. **위치/경로/좌표, 기기 ID, 패키지명, 이름 등 개인 식별 정보 금지.**
- 대화 기록은 기기 DB에만 저장. 전송은 최근 대화 10턴까지만.

## 시스템 지시 (한국어)
- 너는 사용자의 개인 건강 데이터 도우미. 한국어로, 짧고 친근하게, 숫자 근거를 들어 설명.
- 생활 습관·운동·수면 위생 같은 **일반적 조언은 적극적으로** 한다. 단 **진단(병명 판단)·약 추천은 하지 않는다.** 걱정되는 패턴(예: 안정 심박이 지속적으로 매우 높거나 낮음, 극단적 수면 부족이 장기간)은 조언과 함께 의료 전문가 상담을 권한다.
- 측정 한계를 알려 줄 수 있다(수면은 갤럭시 핏3 기준, 버스에서 조는 건 미측정, 체중계 체지방률은 측정 편차가 큼, 걸음은 하루 최댓값 기기 기준 등).
- 데이터가 없으면 지어내지 말고 없다고 말한다. 도구를 사용해 필요한 기간을 조회한다.

## 도구 (functionDeclarations)
1. `get_daily_summary(start_date, end_date)` — yyyy-MM-dd, 최대 120일. 반환: 날짜별 {date, steps, sleep_minutes, hr_avg, hr_min, hr_max, weight_kg, body_fat_pct, bone_mass_kg, lean_mass_kg, body_water_kg, walk_distance_m} (null 생략)
2. `get_period_stats(start_date, end_date)` — 기간 평균/최소/최대/기록일수 (걸음, 수면, 심박 평균, 체중) + 수면 단계 평균(분)
3. `get_sleep_days(start_date, end_date)` — 최대 60일. 날짜별 {date, total_min, sessions, first_start(HH:mm), last_end(HH:mm), deep_min, light_min, rem_min, awake_min}
4. `get_walk_sessions(start_date, end_date)` — 최대 100건. {date, start(HH:mm), distance_m, active_min, pace_min_per_km, steps} (경로 없음)
5. `get_data_range()` — 종류별 가장 오래된/최근 기록 날짜

## 화면
- 하단 탭 6개: 오늘 / 활동 / 걷기 / 수면 / 몸 / AI
- 첫 사용 시 동의 화면(보내는 것/보내지 않는 것 명시, Gemini 무료 등급은 구글이 입력을 서비스 개선에 사용할 수 있음 안내). 동의는 SharedPreferences. 설정 화면에서 동의 철회(철회 시 전송 불가) 가능.
- 채팅: 말풍선, 입력창, 전송, 응답 중 "생각하는 중…" 표시, 추천 질문 칩 3~4개, "대화 지우기". 답변 마크다운은 **굵게**·목록 정도만 간단히 렌더링(라이브러리 없이) 또는 일반 텍스트.
- 오류: 키 없음 / 네트워크 / 429(한도 초과: "잠시 후 다시 시도") / 기타 → 말풍선에 안내.

## DB
- chat_message 테이블 추가(id, role "user"/"model", text, time). **DB version 3 + MIGRATION_2_3**, destructive 금지.

## 공유 계약 (codex-1 ↔ codex-2)
```kotlin
// com.gh.healthapp.chat
data class ChatMessage(val id: Long, val role: String, val text: String, val time: Long)
sealed interface ChatError { data object NoKey: ChatError; data object Network: ChatError; data object RateLimited: ChatError; data class Other(val message: String): ChatError }
class ChatRepository(context: Context) {
  val messages: Flow<List<ChatMessage>>
  suspend fun send(userText: String): Result<Unit>   // 실패 시 Result.failure(ChatException(error: ChatError))
  suspend fun clear()
  fun hasApiKey(): Boolean
}
class ChatException(val error: ChatError): Exception()
object ChatConsent { fun isGiven(context: Context): Boolean; fun set(context: Context, given: Boolean) }
```

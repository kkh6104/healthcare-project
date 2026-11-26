# 구현 요청 (codex-1): Gemini 무료 등급 속도·안정성 최적화
- 수정 허용: chat/ 패키지, 테스트. 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug testDebugUnitTest`. 키·대화 내용 로그 금지.
## 실측 (최근 로그)
- gemini-flash-latest: 거의 매번 503. gemini-flash-lite-latest: 대부분 성공(가끔 스트림 도중 503). gemini-2.5-flash: 404(없음).
- 도구 사용 질문 47초/63초, 최종 생성 단계가 최대 57초.
## 1. 모델 목록 자동 확인
- `GET https://generativelanguage.googleapis.com/v1beta/models?pageSize=200` (헤더 x-goog-api-key). supportedGenerationMethods에 generateContent가 있고 이름에 "flash"가 포함되며 "image"/"tts"/"audio"/"live"/"embedding"이 없는 모델만 후보. 하루 1회 캐시(SharedPreferences, 목록과 시각). 실패 시 기존 고정 목록 사용.
- 후보 순서: "-latest" 별칭 우선(flash-lite-latest, flash-latest), 그다음 버전 높은 순 최대 2개 → 총 최대 4개.
## 2. 성공 기반 순서
- 모델별 최근 결과(성공/붐빔/타임아웃, 응답 시간)를 메모리+SharedPreferences에 기록. 다음 질문은 **최근 30분 내 성공한 모델을 먼저**, 붐빔 쿨다운 모델은 뒤로. 로그 `chat model order=[..]`.
## 3. 보내는 양 줄이기
- RAG 상위 k=3, 유사도 임계값 0.6, 청크 텍스트는 400자까지 자름.
- 최근 7일 컨텍스트: 날짜별 한 줄 CSV 형태(date,steps,sleep_min,hr_avg,weight) — JSON 대신.
- 도구 결과: null 필드 제거, 소수 1자리 반올림, get_daily_summary가 60일 넘게 요청되면 주 단위 평균으로 요약해 반환(필드 period="week").
- 대화 이력 최근 6턴.
## 4. 답 길이
- 시스템 지시: "답은 핵심 위주로 10줄 이내. 사용자가 자세히 요청하면 길게." generationConfig.maxOutputTokens 1024.
## 5. 테스트: 모델 목록 필터·정렬, 성공 기반 순서, 7일 CSV 포맷, 60일 초과 주 단위 요약
## 완료: tasks/chat-free-speed-done.md 1~3줄

# 구현 요청 (claude-2): 몸무게 페이지 반복 무한 루프 수정 (긴급)

## 원인 (실기기 로그 확인됨)
WeightRepository.summary()의 do-while 페이지 루프가 끝나지 않음. 실기기(Android 10, Health Connect APK)는 마지막 페이지에서도 pageToken을 null이 아닌 값(빈 문자열 추정)으로 돌려줘서, 같은 10건을 26초 동안 1,014번 다시 요청 → Health Connect 요청 한도 초과.

## 수정
1. HcLog.kt(또는 공용 위치)에 페이지 루프 공용 함수 추가:
   `suspend fun <T : Record> readAllPages(name: String, maxPages: Int = 5, request: (pageToken: String?) -> ReadRecordsRequest<T>): List<T>`
   - 각 페이지 호출은 hcCall(name, ...)로 감싼다.
   - pageToken이 **null 또는 빈 문자열(isNullOrEmpty)** 이면 종료.
   - **직전 토큰과 같은 토큰**이 오면 종료.
   - maxPages에 도달하면 종료하고 Log.w("HCDEBUG", "[name] maxPages reached") 기록.
2. WeightRepository의 루프를 readAllPages로 교체.
3. **프로젝트 전체에서 pageToken을 쓰는 다른 루프**(Sleep, 진단, 심박 등)가 있으면 모두 readAllPages로 교체. 없으면 그대로.
4. 다른 로직은 건드리지 않는다.

## 완료 조건
- assembleDebug 성공 (PowerShell, JAVA_HOME = C:\Program Files\Android\Android Studio\jbr)
- tasks/fix-weight-loop-done.md 에 2~4줄 + pageToken 사용처 목록

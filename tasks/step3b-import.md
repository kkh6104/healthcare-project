# 구현 요청 (claude-1): 삼성헬스 CSV 가져오기 연결

사용: data/SamsungCsvParser.kt (API: tasks/step3b-csv-parser-done.md, 수정 금지), DailySummaryBuilder.rebuildAll(), HeartRateRetention.

## 0. 선행 수정: 대량 삭제/저장 나누기
- HealthDao.replaceRecords / deleteRecordsByIds 사용 시 SQLite 변수 한도(999)를 넘지 않도록 **500개씩 나눠서** 처리 (한 트랜잭션 안에서 반복). upsertRecords도 호출부에서 500개씩.

## 1. 의존성
- androidx.documentfile:documentfile 최신 안정 버전 (libs.versions.toml).

## 2. 화면 (MainActivity)
- 새로고침 버튼 옆(또는 아래)에 OutlinedButton "삼성헬스 데이터 가져오기".
- 누르면 `ActivityResultContracts.OpenDocumentTree()` 로 폴더 선택. 안내 문구(버튼 아래 작은 글씨): "Download/Samsung Health 안의 samsunghealth_... 폴더를 선택하세요".
- 가져오는 동안: 버튼 비활성 + "가져오는 중… (심박 읽는 중)" 같은 진행 문구.
- 끝나면 배너: "가져오기 완료: 걸음 N일 · 심박 N건 · 수면 N건 · 체중/체성분 N건 (잘못된 기록 N건 제외)". 실패 시 "가져오기 실패: 메시지".

## 3. ViewModel.importSamsung(treeUri)
- Dispatchers.IO에서. DocumentFile.fromTreeUri.
  - 선택한 폴더에 CSV가 없고 하위에 `samsunghealth_`로 시작하는 폴더가 있으면 **이름이 가장 큰(최신)** 하위 폴더 사용.
- 파일명으로 SamsungCsvParser.kindOf 판별 → contentResolver.openInputStream → InputStreamReader(UTF-8, BOM이 있으면 제거)로 파싱. 수면은 SLEEP + SLEEP_STAGE 둘 다 열어 parseSleep.
- 결과 레코드 500개씩 upsert → summaryBuilder.rebuildAll() → retention.run() → DB에서 화면 다시 읽기.
- Health Connect 동기화와 동시에 돌지 않게: 진행 중인 refreshJob이 있으면 끝날 때까지 기다리거나 취소 후 시작 (택1, 로그로 남김).
- 로그(HCDEBUG): `import start`, 파일별 `import <kind> records=n dropped=n ms`, `import end total=n`, `retention: rolled=n`. **개인 수치 값은 로그에 쓰지 말 것(건수만).**
- 같은 폴더를 두 번 가져와도 중복이 생기지 않아야 함 (externalId UNIQUE + REPLACE로 보장되는지 확인).

## 완료
- PowerShell: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug assembleDiag testDebugUnitTest` 성공
- tasks/step3b-import-done.md 에 3~5줄

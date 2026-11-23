# step3b-import 완료
- 선행 수정: HealthDao.replaceRecords가 upsert/삭제를 500개씩 나눠 한 트랜잭션에서 처리, HealthConnectManager.saveRecords가 500개씩 upsert. androidx.documentfile 1.1.0 의존성 추가(libs.versions.toml, app/build.gradle.kts).
- MainActivity: 새로고침 옆에 "삼성헬스 데이터 가져오기" 버튼(OpenDocumentTree), 아래 안내 문구, 진행 중에는 두 버튼 비활성 + "가져오는 중… (심박 읽는 중)" 등 진행 문구, 완료/실패는 배너("가져오기 완료: 걸음 N일 · 심박 N건 · 수면 N건 · 체중/체성분 N건 (잘못된 기록 N건 제외)" / "가져오기 실패: 메시지").
- StepsViewModel.importSamsung(treeUri): IO에서 폴더 탐색(CSV 없으면 가장 큰 samsunghealth_ 하위 폴더), kindOf로 판별, UTF-8(BOM 제거)로 걸음/심박/체중/수면(+단계) 파싱 → 저장 → rebuildAll → retention.run → DB 재조회. 진행 중인 refresh는 취소 후 끝날 때까지 기다린 뒤 시작(로그 "import: cancelling running refresh"). 로그는 건수만(import start / import <kind> records dropped ms / import end total / retention: rolled).
- 중복 방지: 파서의 externalId가 datauuid·날짜 기반으로 결정적이고 (source,type,externalId) UNIQUE + REPLACE라 같은 폴더를 다시 가져와도 중복이 생기지 않음(코드로 확인, 실제 재가져오기는 미실행).
- 빌드: assembleDebug assembleDiag testDebugUnitTest 모두 BUILD SUCCESSFUL (실기기/실제 CSV 가져오기 미확인)

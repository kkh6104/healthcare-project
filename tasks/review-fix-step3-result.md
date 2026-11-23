FAIL
1. OK — 조회·저장과 걸음 집계 성공 후 syncedUntil을 갱신한다.
2. NG — SyncRepository.kt:62,99: 1일 겹침 범위에 5,000건 이상이 있으면 새로고침마다 같은 5페이지를 다시 읽어 이어서 진행하지 못할 수 있다. 오름차순·시각 갱신·배너는 구현되어 있다.
3. OK — SecurityException 시 이전 성공분을 rebuild한 뒤 NeedPermission으로 전환한다.
4. NG — StepsViewModel.kt:62,165-175: 초기 DB 읽기가 끝나기 전에 권한 부족이나 미설치 상태가 설정되면 초기 결과를 버려 저장된 카드가 표시되지 않는다.
5. OK — 지정된 JDK로 assembleDebug 및 assembleDiag 성공 (74개 작업 UP-TO-DATE).

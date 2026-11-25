PASS
1. 키: x-goog-api-key 헤더로만 사용, 로그(tools/ms/result)·오류문구·화면에 키 없음, 키 없으면 NoKey 안내 카드(크래시 없음)
2. 전송: 도구 결과는 날짜별 숫자·수면 시각·걷기 거리/시간/걸음만, 좌표·경로·기기ID·패키지명 없음
3. MIGRATION_2_3: chat_message(id INTEGER PK AUTOINCREMENT NOT NULL, role/text TEXT NOT NULL, time INTEGER NOT NULL)가 엔티티와 일치, MIGRATION_1_2 유지
4. 루프: model 턴 추가 후 role "user" + parts[{functionResponse:{name,response}}], 도구 왕복 최대 4회 후 오류로 종료
5. 동의 없으면 네트워크 전 차단, 429/IOException/401·403 분류
6. assembleDebug·testDebugUnitTest BUILD SUCCESSFUL
참고: Gemini는 잘못된 키에 400(API_KEY_INVALID)을 주는 경우가 많아 "서버 오류 (400)"로 보일 수 있음

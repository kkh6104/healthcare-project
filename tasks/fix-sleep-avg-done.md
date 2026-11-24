# fix-sleep-avg 완료
- 원인(추정): DetailReader.sleepNights/metricHistory가 기간을 minusDays(days)로 잡아 오늘 포함 N+1일을 읽고, 수면은 시작 시각 기준으로만 걸러서 선택 기간 밖 날짜가 평균·최장·최단·목록에 섞였음. 30일 칩이 31일, 90일 칩이 91일을 쓰던 셈이라 값이 어긋남. (칩 변경 시 데이터를 다시 읽는 코드는 정상이어서 "90일 데이터로 계산"이라는 가설은 코드로 확인되지 않았고, 실기기에서 재현은 못 함)
- 수정: sleepNights는 깬 날짜가 [오늘-(N-1), 오늘]인 세션만 사용(전날 밤에 시작한 세션을 받으려고 읽기 시작만 하루 앞당김), metricHistory도 오늘 포함 정확히 N일. 그래프·평균·최장/최단·목록이 같은 sleepDays 데이터를 씀.
- 확인: 걸음 상세(StepsDetail)는 stepsDaily(days)가 이미 정확히 N일이고 평균·최고·달성일이 같은 list에서 계산되어 수정 불필요. 체중/체성분은 metricHistory 수정으로 같이 해결.
- 빌드: assembleDebug BUILD SUCCESSFUL. 실기기에서 30일/90일 평균이 DB 계산값(4시간 22분 / 4시간 19분 부근)과 맞는지 확인 필요.

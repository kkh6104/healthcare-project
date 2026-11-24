# walk-thumb 완료
- 새 파일 ui/walk/RouteThumbnail.kt(Canvas 선 그림: 경도 cos(위도) 보정, 비율 유지, 8dp 여백, primary 3dp 선 + 시작 secondary 점/끝 primary 점, surfaceVariant 둥근 배경, 점 2개 미만이면 "경로 없음"). 점이 1개일 때도 "경로 없음".
- ui/walk/WalkTab.kt 목록의 RouteMap 썸네일을 RouteThumbnail로 교체하고 상위 3개 제한 제거(모든 항목에 표시, 항목별 produceState로 DB 읽기만). 진행 중 화면의 RouteMap은 그대로. assembleDebug 성공.

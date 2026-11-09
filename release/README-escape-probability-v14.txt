escape probability v14

JDK/Java 17 권장.
Linux: 이 폴더의 run-linux.sh 실행.
Windows: run-windows.bat (이번 검증은 Linux에 한정).

Enter 시작/다음 건물, A/D 또는 방향키 이동, Space/위쪽 점프,
P/Esc 정지/재개, R 새 게임, 상점 1=쿠션 / 2=커피.
포커스를 잃으면 정지하며 돌아와서 직접 재개해야 합니다.

10층 → 상점 → 20층(화염) → 상점 → 30층(화염·붕괴·연기).
한 번에 n층 낙하할 때 생존확률은 0.9^(n*(n+1)/2).
동영상은 docs/escape-probability-v14-demo-60s.mp4에 있습니다.
실제 자동 입력·구간 편집이며 마지막 12초는 0.5배속입니다.

자세한 규칙/빌드: 상위 폴더 README.md.
실제 검증/제한: docs/ESCAPE_PROBABILITY_V14_RESULT.md.

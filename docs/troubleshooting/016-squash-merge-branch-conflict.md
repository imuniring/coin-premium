# 016. squash merge 후 새 PR에서 충돌 발생

- 날짜: 2026-11-15
- 관련: #36 (환율 반영 로그 복구 PR)

## 증상
이전 PR을 squash merge한 뒤 새 브랜치에서 작은 수정 PR을 올렸는데,
main과 같은 파일·같은 줄에서 충돌이 났다.

## 원인
새 브랜치를 최신 main이 아니라 **이전 작업 브랜치**에서 땄다.
squash merge는 브랜치 커밋들을 **새 커밋 하나로 합쳐** main에 올리므로,
내용이 같아도 Git은 이전 브랜치의 커밋과 main의 squash 커밋을 다른 변경으로 본다.

## 해결
마지막 커밋(수정분)만 최신 main 위로 옮겼다.

```bash
git fetch origin
git log --oneline origin/main..HEAD        # main에 없는 커밋 확인
git rebase --onto origin/main HEAD~1       # 마지막 커밋만 main 위로
git push --force-with-lease
```

## 배운 점
- 새 작업은 항상 `git switch main && git pull` 후 브랜치를 딴다.
- 머지된 작업 브랜치는 로컬에서도 삭제한다 (squash merge된 브랜치는 `-D` 필요).
- `--force` 대신 `--force-with-lease`로 원격의 예상치 못한 변경을 덮어쓰지 않는다.

# 013. 커밋 메시지의 `!` 때문에 zsh 에러

- 날짜: 2026-11-11

## 증상
```bash
git commit -m "feat(premium)!: v1 경로 변경 및 에러 응답 표준화"
zsh: illegal modifier:
```

## 원인
zsh·bash에서 `!`는 히스토리 확장(이전 명령 불러오기) 기호이며, 큰따옴표 안에서도 동작한다. Breaking Change 표시(`feat!:`)의 `!:`를 히스토리 명령으로 해석했다.

## 해결
작은따옴표로 감싼다. 작은따옴표 안의 특수 기호는 글자 그대로 취급된다.
```bash
git commit -m 'feat(premium)!: v1 경로 변경 및 에러 응답 표준화'
```

## 참고
- `!`는 Conventional Commits에서 하위 호환이 깨지는 변경 표시. 본문에 `BREAKING CHANGE:`로 내용을 적을 수도 있다

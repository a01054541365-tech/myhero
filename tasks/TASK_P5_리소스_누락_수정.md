# TASK P5: 리소스 누락 수정

> 우선순위: 🟡 낮음 (1단계 — HOTFIX 완료 후 시작, 병렬 가능)  
> 소스셋: resources  
> 완료 기준: `./gradlew compileJava` 성공 + 클라이언트에서 아이템 모델 정상 표시

---

## 의존 관계

- **선행 필수:** HOTFIX 완료
- 다른 P 태스크와 병렬 가능

---

## 수정 항목

### 1. 아이템 모델 JSON 12개 추가
- 누락된 아이템 12개의 모델 JSON 파일 생성
- 경로: `src/main/resources/assets/jjk/models/item/`
- 각 아이템에 대응하는 텍스처 참조 확인

누락 아이템 목록 (확인 필요):
- [ ] 아이템 1
- [ ] 아이템 2
- [ ] 아이템 3
- [ ] 아이템 4
- [ ] 아이템 5
- [ ] 아이템 6
- [ ] 아이템 7
- [ ] 아이템 8
- [ ] 아이템 9
- [ ] 아이템 10
- [ ] 아이템 11
- [ ] 아이템 12

### 2. 애니메이션 더미 파일
- 누락된 애니메이션 `.animation.json` 더미 파일 추가
- GeckoLib 4.8.3 형식 준수 (4.8.4 Forge 전용 — 절대 사용 금지)
- 경로: `src/main/resources/assets/jjk/animations/`

### 3. 쉐이더 조사
- 누락 또는 오류 있는 쉐이더 파일 조사
- 필요 시 더미 또는 수정 파일 추가
- 경로: `src/main/resources/assets/jjk/shaders/`

---

## 검증
```
./gradlew compileJava
클라이언트 접속 → 아이템 12개 모델 정상 표시 확인
콘솔에 missing model / missing texture 경고 없음 확인
```

---

> **P5 완료 후: tasks/ 폴더의 모든 .md 파일 삭제**

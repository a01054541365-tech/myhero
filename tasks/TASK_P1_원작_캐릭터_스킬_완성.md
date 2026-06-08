# TASK P1: 원작 캐릭터·스킬 완성

> 우선순위: 🔴 높음 (1단계 — HOTFIX 완료 후 시작)  
> 소스셋: main  
> 완료 기준: `./gradlew compileJava` 성공

---

## 의존 관계

- **선행 필수:** HOTFIX 완료
- **후행 차단:** P3 (bloodResource 마이그레이션 버전 확정 필요)

---

## 수정 항목

### 1. 쵸소 (Choso) 캐릭터 추가
- `PlayerData`에 `bloodResource` 필드 추가
- `snapshot()` 복사 라인 추가 필수
- `DbMigrator.java` CURRENT_VERSION +1 + ALTER TABLE
- ISkillSet 5개 키 (F, Shift+F, R, Shift+R, V) 전부 구현
- 미사용 키는 `NOT_IMPLEMENTED` 반환

### 2. 세계절단 (World Slash) 스킬
- 명세서 §6 기준으로 구현
- CombatPipeline 9단계 순서 준수

### 3. 완전부활 (Complete Recovery) 스킬
- 명세서 §6 기준으로 구현

### 4. 옷코츠 영역 재분류
- 옷코츠 영역을 올바른 카테고리로 재분류
- DomainInstance class 유지 (record 금지)

---

## DB 마이그레이션

```
현재 CURRENT_VERSION 읽기 → +1
ALTER TABLE player_data ADD COLUMN blood_resource INTEGER DEFAULT 0;
```

## 검증
```
./gradlew compileJava
쵸소 캐릭터 선택 → 스킬 5개 키 동작 확인
세계절단 / 완전부활 발동 확인
```

---

> 완료 후 P3 시작 가능 (버전 번호 확정 후 P3에 전달)

# TASK P3: 경제 시스템 + CE Control 성장 경로 + 주구 기초

> 우선순위: 🟠 중간 (2단계 — P1·P4 완료 후 시작)  
> 소스셋: main  
> 완료 기준: `./gradlew compileJava` 성공

---

## 의존 관계

- **선행 필수:** P1 완료 (bloodResource 마이그레이션 버전 확정)
- **선행 필수:** P4 완료 (questProgress/seasonXp 마이그레이션 버전 확정)
- P3에서 추가 마이그레이션 수행 (버전 충돌 방지)

---

## 수정 항목

### 1. CE결정체 화폐 시스템
- CE결정체를 인게임 화폐로 사용하는 경제 시스템 구현
- 아이템 정의 및 거래 로직
- `PlayerData` CE결정체 관련 필드 추가 (있으면)
  - `snapshot()` 복사 라인 추가 필수
  - `DbMigrator.java` CURRENT_VERSION +1 + ALTER TABLE

### 2. CE Control 성장 경로
- CE Control 레벨업 경로 및 수치 명세 기반 구현
- 성장에 따른 CE 재생/최대치 변화

### 3. 주구 (Juku) 기초 드롭
- 주구 아이템 드롭 시스템 기초 구현
- `fingerDropRate = 0.10` §LOCK 값 유지
- `fingerMaxCount = 20` §LOCK 값 유지

---

## DB 마이그레이션

```
P1의 CURRENT_VERSION + P4의 버전 증가분 이후에 수행
현재 CURRENT_VERSION 읽기 → +1
(CE결정체 필드가 있는 경우 ALTER TABLE 추가)
```

## 검증
```
./gradlew compileJava
CE결정체 획득 → 거래 동작 확인
CE Control 성장 동작 확인
주구 드롭률 확인 (fingerDropRate=0.10)
```

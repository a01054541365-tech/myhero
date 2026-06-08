# HOTFIX: CursedSpiritEntity NPE 크래시

> 우선순위: 🔴 즉시 (0단계 — 이것 없이 서버가 월드 진입 직후 종료됨)  
> 소스셋: main  
> 완료 기준: `./gradlew compileJava` 성공 + 서버 월드 진입 정상 확인

---

## 문제

- `CursedSpiritEntity` 초기화 중 NPE 발생 → 서버 크래시
- config 파일에 누락된 키로 인한 추가 오류

## 수정 항목

### 1. CursedSpiritEntity NPE
- 엔티티 생성 시 null 참조 발생 지점 파악 후 수정
- null-safe 초기화 보장

### 2. config 누락 키 수정
- 서버 시작 시 config 로드 중 누락된 키 감지
- 기본값 fallback 또는 키 추가로 수정

## 수정 대상 파일 (추정)
- `src/main/.../entity/CursedSpiritEntity.java`
- `src/main/.../config/JjkConfig.java` 또는 config JSON

## 검증
```
./gradlew compileJava
서버 기동 → /jjk join → 월드 진입 → 크래시 없음 확인
```

---

> 완료 후 TASK_INDEX.md의 0단계 체크

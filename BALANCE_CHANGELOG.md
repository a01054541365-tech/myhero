# Balance Changelog — TASK B

작업 기준일: 2026-06-05
대상 파일: run/config/jjk/techniques.json, run/config/jjk/config.json, src/main/java/com/jjk/entity/CursedSpiritGrade.java
DamageCalculator.java 수정 없음 확인.

---

## techniques.json

### 고죠 사토루 (S 티어)
| 스킬 | 필드 | 변경 전 | 변경 후 |
|------|------|---------|---------|
| purple (무라사키) | baseDamage | 75 | 65 |
| purple (무라사키) | ceCost | 480 | 520 |
| blue (창) | baseDamage | 31 | 28 |
| blue (창) | ceCost | 400 | 360 |
| red (혁) | baseDamage | 38 | 34 |
| red (혁) | ceCost | 400 | 360 |
| unlimited_void (무량공처) | ceCost | 5600 | 6000 |

### 료멘 스쿠나 (S 티어)
| 스킬 | 필드 | 변경 전 | 변경 후 |
|------|------|---------|---------|
| dismantle (해) | baseDamage | 48 | 42 |
| dismantle (해) | ceCost | 360 | 400 |
| cleave (팔, 타당) | baseDamage | 18 | 16 |
| arrow (화염화살) | baseDamage | 30 | 26 |
| malevolent_shrine (복마어주자) | ceCost | 4200 | 4800 |

### 옷코츠 유타 (A 티어)
| 스킬 | 필드 | 변경 전 | 변경 후 |
|------|------|---------|---------|
| rika_summon | baseDamage | 58 | 52 |
| sword_slash (검격) | baseDamage | 72 | 60 |
| sword_slash (검격) | cooldownTicks | 10 | 12 |
| rika_burst | burst_multiplier | 1.30 | 1.25 |

### 마히토 (A 티어)
| 스킬 | 필드 | 변경 전 | 변경 후 |
|------|------|---------|---------|
| blade_transfiguration (영혼의 칼날) | baseDamage | 34 | 30 |
| blade_transfiguration (영혼의 칼날) | ceCost | 280 | 300 |

### 죠고 (B 티어) — 이번 세션에서 직접 수정
| 스킬 | 필드 | 변경 전 | 변경 후 |
|------|------|---------|---------|
| 화산탄 | baseDamage | 44 | 42 |
| 개관 | baseDamage | 78 | 76 |
| 운석 | baseDamage | 118 | 116 |
| 불꽃의 고리 | baseDamage | 52 | 50 |

### 나나미 켄토 (B 티어)
| 스킬 | 필드 | 변경 전 | 변경 후 |
|------|------|---------|---------|
| 십획주법 (7:3 분할) | baseDamage | 29 | 32 |
| nanami_overtime (시간 외 근무) | ceCost | 250 | 220 |

### 이타도리 유지 (C 티어)
| 스킬 | 필드 | 변경 전 | 변경 후 |
|------|------|---------|---------|
| divergent_fist (경정권) | baseDamage | 34 | 36 |
| manji_kick (맨지 킥) | baseDamage | 44 | 40 |
| manji_kick (맨지 킥) | cooldownTicks | 7 | 6 |

### 후시구로 메구미 (C 티어)
| 스킬 | 필드 | 변경 전 | 변경 후 |
|------|------|---------|---------|
| nue (누에) | baseDamage | 38 | 40 |
| divine_dog (옥견) | baseDamage | 32 | 34 |

### 히구루마 히로미 (C 티어)
| 스킬 | 필드 | 변경 전 | 변경 후 |
|------|------|---------|---------|
| submit_evidence (심판) | baseDamage | 30 | 28 |
| submit_evidence (심판) | ceCost | 200 | 180 |

### 하카리 킨토키 (B 티어)
변경 없음 — 잭팟 조건부 설계 의도 유지.

### 이누마키 토게 (D 티어)
변경 없음 — CC 중심 서포터, 딜 조정 불필요.

---

## config.json

| 필드 | 변경 전 | 변경 후 | 이유 |
|------|---------|---------|------|
| nonSorcMult | 2.0 | 1.8 | ATK 스탯 없이 PvP 과다 피해 억제 |
| nonSorcMultExtreme | 2.6 | 2.2 | 주술사와 격차 유지하되 과도한 배율 하향 |

---

## CursedSpiritGrade.java

변경 없음 — 이미 명세 기준과 일치:

| 등급 | HP | attackDamage |
|------|----|----|
| GRADE_4 (4급) | 30 | 4 |
| GRADE_3 (3급) | 60 | 8 |
| GRADE_2 (2급) | 100 | 14 |
| GRADE_1 (1급) | 180 | 20 |
| SEMI_SPECIAL (준특급) | 280 | 28 |
| SPECIAL (특급) | 400 | 38 |

---

## 검증 결과

- ./gradlew compileJava: BUILD SUCCESSFUL
- techniques.json JSON lint (Node.js): OK
- DamageCalculator.java 수정 없음 확인

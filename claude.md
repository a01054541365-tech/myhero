# CLAUDE.md — JJK Fabric Mod 작업 지침서
> Fabric 1.21.1 · Java 21 · Dedicated Server  
> 세션 시작 시 이 파일을 최우선으로 읽는다.  
> JJK 전용 규칙과 범용 규칙이 충돌하면 JJK 전용 규칙이 우선한다.

---

## A. 기준 문서 우선순위

| 순위 | 파일 | 역할 |
|---|---|---|
| 1 | `docs/jjk_spec_v5.md` | 최종 권위 문서. 모든 수치의 기준. |
| 2 | `docs/jjk_spec_01_core.txt` ~ `docs/jjk_spec_05_presentation.txt` | 분야별 세부 명세 |
| 3 | `docs/jjk_spec_06_decisions.md` | 확정된 설계 결정 기록 |

수치가 파일 간 충돌하면 → 코딩 전에 사람에게 확인 요청. 임의 선택 금지.

---

## B. §LOCK — 절대 변경 금지 수치 (명시적 지시 없이 수정 시 즉시 중단)

| 항목 | 값 | 근거 |
|---|---|---|
| PvP 피해 상한 | `max_hp × 0.40` | §3-4 |
| 흑섬 데미지 배율 | `base × 2.5` | §3-2 (지수승 아님, 게임화 확정) |
| 각성 발동 HP | `maxHp × 0.30` | §3-7 |
| 각성 지속 | `160틱 (8초)` | §3-7 |
| 각성 쿨타임 | `2400틱 (120초)` | §3-7 |
| 속박 타임아웃 | `300틱 (15초)` | §3-1 |
| Zone 지속 | `200틱 (10초)` | decisions §2-3 |
| 흑섬 Just Frame | `8~12틱` | §3-5 |
| 흑섬 baseRate | `5%` | config §12-1 |
| 흑섬 zoneBonus | `10%` | config §12-1 |
| 히구루마 재판 성공률 | `0.60` | §23 |
| 잭팟 기본값 상한 | `251틱` | decisions §3-5 |
| CE 전투 외 재생 | `1.0/틱` | decisions §2-4 |
| CE 전투 중 재생 | `0.2/틱` | decisions §2-4 |
| sealDurationTicks | `600틱 (30초)` | decisions §2-2 |
| maharagaThreshold | `5회` | decisions §2-1 |
| rikaLifetimeTicks | `200틱` | decisions |
| fingerDropRate | `0.10` | config §12-1 |
| fingerMaxCount | `20` | config §12-1 |
| 개방형 영역 자박 CE | `ceCost × 2` | decisions §3-4 |
| domains.json wallHp (기본) | `1500` | §8-3 |
| TickDamageCap PvP | `min(finalDamage, hpMax × 0.40f)` | §3-4 |

---

## C. 소스셋 분리 원칙 — 위반 시 dedicated server 즉시 크래시

```
src/main/   ← 서버 + 공통. client-only 클래스 import 절대 금지.
src/client/ ← 클라이언트 전용. 서버에서 절대 참조 금지.
```

**main 소스셋 import 절대 금지:**
- `GeoEntityRenderer`, `GeoModel`, `GeoReplacedEntityRenderer`
- `PlayerAnimator`, `AnimationController`, `SkillAnimController`
- `DomainBoundaryRenderer`, `CEBarRenderer`, `SkillCooldownHUD`, `BlackFlashCinematic`
- `MinecraftClient`, `WorldRenderer`, `GameRenderer`, `Screen`
- `ParticleEffect` 직접 spawn (반드시 S2C 패킷으로 트리거)
- `@Environment(EnvType.CLIENT)` 없이 렌더 관련 클래스 전체

**main 소스셋에 있어야 하는 것 (헷갈리는 것):**
- `GeoEntity` implements + `AnimatableInstanceCache` → 서버 엔티티 클래스에 포함 가능 (렌더러 아님)
- 모든 Manager, SkillSet, CombatPipeline, DomainManager, CEManager, TickScheduler
- 모든 C2S/S2C 패킷 코덱 (Codec 자체는 server-side)

---

## D. 절대 금지 10항

1. `client-only` 클래스를 `main` 소스셋에 import
2. Physics Mod 서버 API 사용 (서버 사이드 API 없음 — 구현 자체가 불가)  
   → 대체: `FallingBlockEntity` spawn + 커스텀 S2C 파티클 패킷
3. 명세서에 없는 기능 임의 추가
4. §LOCK 수치 임의 조정
5. `DomainInstance`를 `record`로 선언 (`wallHp`·`currentRadius`는 가변 필드, class 필수)
6. GeckoLib `4.8.4` 사용 (Fabric 전용 `4.8.3`만 허용. 4.8.4 = Forge 전용, ClassNotFoundException 크래시)
7. 의사코드(pseudocode) 제출 — 반드시 컴파일 가능한 완전한 Java
8. PlayerData 필드 추가 시 `snapshot()` 복사 + DB migration 생략
9. Mixin 추가 시 `src/main/resources/jjk.mixins.json` 동시 수정 생략
10. `[작성 필요]` 항목을 임의 수치로 채워서 진행

---

## E. CombatPipeline 순서 — 레이스 컨디션 방지, 절대 변경 금지

```
1단계  입력 검증       TeamManager(진영), characterId 소유 여부
2단계  CE·쿨타임 검증  CEManager, cooldowns Map — 쿨타임 검증은 2단계 (decisions §3-1)
3단계  명중·거리·시야  서버 측 계산
4단계  baseDamage 산출 techniques.json 기반
5단계  흑섬·Zone·각성  AwakeningManager.checkAndActivate(data, hpCurrent, hpMax, tick)
6단계  방어·저항 처리  InfinityHandler, isSoulDirect(→effectiveDefense=0), NON_SORCERER 배율
7단계  최종 배율 clamp ×0.25 ~ ×4.0 (각성 ×1.25, burstActive ×1.30 포함)
8단계  TickDamageCap   min(finalDamage, target.hpMax × 0.40f)
9단계  반영 + S2C 전송 SkillResultS2CPacket
```

CooldownManager 없이 어떤 스킬도 발동 금지.  
`CombatPipeline.process()` 외부에서 `AwakeningManager.checkAndActivate()` 호출 금지.

---

## F. PlayerData 규칙

**필드 추가 시 반드시 3개 동시 작성:**
1. `PlayerData` 필드 선언
2. `snapshot()` 복사 라인 추가
3. `Migrator.java` schemaVersion 증가 + ALTER TABLE

**전용 필드 (cooldowns Map 키로 절대 사용 금지):**
- `domainCooldownUntil` → PlayerData 전용 필드 (decisions §3-2)
- `awakeningCooldownUntil` → PlayerData 전용 필드 (decisions §3-3)
- `bindingVowDeclaredTick` → PlayerData 전용 필드, 미선언 sentinel = `-1L`

**saveAsync() 사용 시 필수:**
`snapshot()` 방어 복사 후 직렬화. 원본 PlayerData 직접 전달 금지 (torn read 방지).

---

## G. Mixin 규칙

- `@Inject at=` 반드시 명시 (HEAD 또는 TAIL 명확히)
- `cancellable=true` 남용 금지 — `InfinityHandler`, `TickDamageCap` 처리용에만
- Mixin 추가 시 `jjk.mixins.json` 동시 수정 필수
- Mixin은 얇게 유지. Manager 직접 호출 금지 → `TickScheduler`에 등록된 작업만 실행
- 현재 등록된 Mixin 3개: `ServerPlayerEntityMixin`, `LivingEntityMixin`, `ServerWorldMixin`

---

## H. 패킷 규칙 (Fabric 1.21.1)

- `PacketByteBuf` 방식 deprecated → `CustomPayload + PacketCodec` 필수
- ID 형식: `jjk:<snake_case>`
- S2C 파티클/이펙트는 패킷으로만 트리거. 서버에서 직접 파티클 spawn 금지
- 클라이언트가 보낸 데미지·명중·쿨타임 값 신뢰 금지

---

## I. 저장 원칙

**즉시 저장 (P0 이벤트):** 손가락 획득 · 캐릭터 선택 · 사망 · 처형검 획득 · 영역 전개 시작/종료  
**주기 저장 가능 (P1 이벤트):** 일반 CE·HP 변화

---

## J. 세션 프롬프트 (작업 시작 시 해당 번호 추가)

| 번호 | 용도 | 추가 프롬프트 |
|---|---|---|
| 1 | 서버 공통 | `Fabric 1.21.1 기준. 서버 권위(authoritative)로 구현하고 클라이언트 패킷은 표시 전용.` |
| 2 | 전투 | `§3·§4 기준. CombatPipeline 9단계 순서와 TickDamageCap을 지키고 §LOCK 수치를 바꾸지 말 것.` |
| 3 | 캐릭터 | `§6 기준. ISkillSet 5개 키(F, Shift+F, R, Shift+R, V)를 모두 구현. 미사용 키는 NOT_IMPLEMENTED 반환.` |
| 4 | 영역 | `§8 기준. DomainInstance는 class이며 wallHp와 currentRadius는 가변 필드. record 선언 금지.` |
| 5 | 저장 | `§13 기준. PlayerData 필드 추가 시 snapshot·schema migration·테스트 동시 반영.` |
| 6 | QA | `§18 기준. 변경한 시스템 단위 테스트와 통합 스모크 테스트를 추가.` |
| 7 | Mixin | `§1-6 기준. @Inject at= 반드시 명시. cancellable=true 남용 금지. jjk.mixins.json 동시 수정.` |
| 8 | 게임 플로우 | `§26 기준. 캐릭터 선택·부활·입장 플로우 준수. 손가락 드롭은 atomic 처리 필수.` |

---

## K. 코딩 일반 원칙

**코딩 전:**
- 가정 금지. 불확실하면 먼저 질문.
- 해석이 여러 개면 제시하고 선택받음. 조용히 선택 금지.
- 더 단순한 방법 있으면 말함. Push back 가능.

**최소주의:**
- 요청한 것만 작성. 추측성 기능 추가 금지.
- 200줄이 50줄로 줄면 다시 작성.
- "flexibility" / "configurability" 미요청 시 추가 금지.

**외과적 수정:**
- 건드린 코드만 수정. 인접 코드 개선 금지.
- 내가 만든 orphan(import/변수/함수)만 정리.
- 기존 dead code는 언급만, 삭제 금지.

**멀티스텝 작업:**
계획 먼저 제시: `[단계] → verify: [검증 방법]`

---

## L. Phase 1 완료 기준 (이 테스트 전체 통과 전 Phase 2 착수 금지)

`DamageCalculatorTest` · `DomainManagerTest` · `PlayerDataSnapshotTest` · `PacketCodecTest` ·
`AwakeningManagerTest` · `ComboTrackerTest` · `FingerSystemTest` · `TeamManagerTest` ·
`CombatPipelineIntegrationTest` · `MigratorTest` · `CooldownKeyConflictTest` · `ISkillSetTest`

TPS 18+ 실측 확인 없이 Phase 2 착수 금지.

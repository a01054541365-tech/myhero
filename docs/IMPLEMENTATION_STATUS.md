# JJK Mod — 구현 현황

> 최종 갱신: 2026-06-13
> 기준: docs/jjk_spec_v5.md (1순위) + 코드 실측 + 317개 테스트 통과 (기존 312 + TASK-1 신규 5)
> ⚠️ 이 문서는 참조용. 수치 권위는 techniques.json / config.json / decisions.md

---

## A. 핵심 명세 — 전부 구현·검증

> A 항목은 코드 실측 + 단위/통합 테스트로 검증된 핵심 시스템이다.

| 시스템 | 구현 위치 | 검증 테스트 | 상태 |
|---|---|---|---|
| CombatPipeline 9단계 + TickDamageCap(PvP ×0.40) | `combat/CombatPipeline` | `CombatPipelineIntegrationTest`·`DamageCalculatorTest` | ✅ |
| DamageCalculator (clamp ×0.25~4.0, 흑섬 ×2.5) | `combat/DamageCalculator` | `DamageCalculatorTest` | ✅ |
| DomainManager (`DomainInstance` = class, 가변 wallHp·radius) | `domain/DomainManager` | `DomainManagerTest` | ✅ |
| CEManager (전투 외 1.0/틱·전투 중 0.2/틱·드레인) | `ce/CEManager` | `UnlimitedVoidCeDrainTest` | ✅ |
| AwakeningManager (HP 0.30 발동·160틱·2400틱 쿨) | `awakening/AwakeningManager` | `AwakeningManagerTest` | ✅ |
| FingerSystem (드롭 0.10·max 20·atomic) | `finger/` | `FingerSystemTest` | ✅ |
| TeamManager (3진영: 주술사·주령·비술사) | `team/TeamManager` | `TeamManagerTest` | ✅ |
| ComboTracker (흑섬·Zone 보너스) | `zone/ComboTracker` | `ComboTrackerTest` | ✅ |
| PlayerData snapshot + Migrator (schema migration) | `data/PlayerData`·`data/Migrator` | `PlayerDataSnapshotTest`·`MigratorTest` | ✅ |
| 패킷 코덱 (CustomPayload + PacketCodec) | `network/` | `PacketCodecTest` | ✅ |
| CooldownManager (키 충돌 방지·전용 필드 분리) | `combat/CooldownManager` | `CooldownKeyConflictTest` | ✅ |
| ISkillSet 12 캐릭터 5키 전부 라우팅 | `character/impl/*` | `ISkillSetTest` | ✅ |
| 히구루마 재판 (성공률 0.60·StateMachine) | `trial/` | `HigurumaTrialTest`·`TrialManagerTest`·`TrialStateMachineTest` | ✅ |
| Grade 체계 (등급 차 PvP scaling) | `grade/` | `GradeEnumTest` | ✅ |
| 비술사(todo) CE 0 기믹 | `character/impl/TodoSkillSet`(외) | `NonSorcererSkillTest` | ✅ |
| Rika / 주령 엔티티 | `entity/`·`RikaEntity` | `RikaEntityTest`·`CursedSpiritEntityTest` | ✅ |
| 스쿠나·나나미·쵸소 스킬셋 | `character/impl/{Sukuna,Nanami,Choso}SkillSet` | `SukunaSkillSetTest`·`NanamiSkillSetTest`·`ChosoSkillSetTest` | ✅ |

---

## B. 명세 초과 구현

> 핵심 명세(§1~§26) 외에 추가 구현되어 테스트로 검증된 시스템.

| 시스템 | 구현 위치 | 검증 테스트 |
|---|---|---|
| 던전 시스템 | `dungeon/DungeonManager` | `DungeonManagerTest` |
| 주간 퀘스트 | `quest/` | `WeeklyQuestTest` |
| 저주석(CursedStone) | `world/`·`item/` | `CursedStoneManagerTest` |
| 주구(CursedTool) 아이템 | `item/` | `CursedToolItemTest` |
| NPC 서비스 | `npc/NpcService` | `NpcServiceTest` |
| 롤백 커맨드 (`/jj` 관리) | `command/` | `RollbackCommandTest` |
| 구조물 빌더 (`/jj build city`) | `world/StructureBuilder` | `StructureBuilderTest` |
| 릴리즈 게이트 | `audit/`·`security/` | `ReleaseGateTest` |
| 안정성·통합 스모크 | — | `StabilityIntegrationTest`·`IntegrationSmokeTest` |
| 부가 시스템 (보스바 CE·커튼·부담·영창·경제·Discord·Advancement) | `bossbar/`·`curtain/`·`burden/`·`chant/`·`economy/`·`discord/`·`advancement/` | — |

---

## C. 미구현 / 부분 구현 항목

| # | 항목 | 상태 | 비고 |
|---|------|------|------|
| 1 | 쵸소 Shift+R 혈도폭쇄 | ✅ TASK-1 완료 | animId 66, techniques.json 등록 완료 |
| 2 | 비술사(todo) 데이터 주도 | △ 보류 | animId 67~71 예약 완료(TASK-2). Blockbench 완료 후 전환 |
| 3 | choso/todo animId 전용화 | △ 예약 완료 | animId 60~66(choso), 67~71(todo) 등록. keyframe은 Blockbench 후 |
| 4 | 이타도리 shrine baseDamage | ✅ 일치 | extendedSkills JSON = 42, 코드 = 42f. 불일치 없음 |
| 5 | 흑섬 grayscale 셰이더 | △ 근사 | 검은 오버레이 플래시로 근사. 정밀화는 GameRenderer post-shader + client Mixin 필요 |
| 6 | 봉인 스킬 슬롯 HUD | △ 대체 | 화면 상단 봉인 인디케이터로 대체. SealedSkillSyncS2CPacket keyId 추가 시 가능 |
| 7 | 구조물 ambient / TPS 파티클 | △ 근사 | 정밀화는 S2C 신설 필요 |

---

## D. 문서-코드 불일치 (기능 갭 아님)

> 기능은 정상 동작하나 문서 기재와 코드 실측이 어긋나는 항목. 수치 권위는 항상 런타임 config/JSON.

- **밸런스 패스 2026-06-11**: 운영자 §LOCK 해제 후 전면 조정. spec_v5/decisions의 일부 구버전 수치는 정정되었으나, 최종 권위는 `techniques.json`·`config.json`·`domains.json`·`FABLE_NOTES.md`(밸런스 패스 섹션)이다.
- **CE 비율별 색상 기준**: decisions 문서 미등재. 사실상 표준은 서버 보스바 `CeBossBarManager.colorFor()`(70% BLUE / 40% GREEN / 20% YELLOW / 미만 RED).
- **todo animId placeholder**: techniques.json의 todo keyId 0~4는 animId 64를 공유(placeholder). Blockbench 모델 완성 후 67~71로 교체 예정 (수치는 이번 미변경).
- **AnimationRegistry**: spec_v5 §26-6은 0~59만 기재. 실제 코드는 60~71까지 등록됨.

---

## 다음 작업 후보 (우선순위 순)

1. Blockbench 모델 완성 → todo animId 67~71 keyframe 연결
2. C-5 흑섬 grayscale post-shader 구현
3. C-6 SealedSkillSyncS2CPacket keyId 필드 추가

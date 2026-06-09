package com.jjk.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerData {

    // === 기본 정보 ===
    public UUID uuid;
    public String characterId;
    public Grade grade;
    public long xp;
    public int mastery;

    // === 전투 스탯 ===
    public float ceCurrent;
    public float ceMax;
    public float hpCurrent;
    public float hpMax;
    public int attackStat;
    public int defenseStat;
    public int speedStat;

    // === 진행 데이터 ===
    public int fingerCount;
    public List<String> unlockedSkills = new ArrayList<>();
    public Map<String, Long> cooldowns = new HashMap<>();

    // === 쿨타임 영속 필드 ===
    public long bindingVowDeclaredTick; // 미선언 = -1L
    public long domainCooldownUntil;
    public long jackpotCooldownUntil;
    public long curtainCooldownUntil;

    // === 상태 ===
    public String trialState;   // "IDLE"|"ACCUSED"|"DELIBERATION"|"VERDICT"|"END"
    public int burden;
    public boolean zoneActive;
    public long zoneEndTick;
    public long lastCombatTick;
    public String lastKnownIp;

    // === 각성 ===
    public boolean awakeningActive;
    public long awakeningEndTick;
    public long awakeningCooldownUntil;

    // === 옷코츠 주력해방 ===
    public boolean burstActive;
    public long burstEndTick;

    // === 메구미 식신 ===
    public List<String> deadShikigamiIds = new ArrayList<>();

    // === 회복 ===
    public boolean healingActive;

    // === Zone 페널티 ===
    public long zonePenaltyUntilTick;

    // === Zone 진입 틱 (진입 틱 공격에 Zone 보너스 미적용) ===
    public long zoneEntryTick = -1L;

    // === 콤보 추적 (Just Frame 흑섬 판정용) ===
    public long lastAttackTick = -1L;

    // === 히구루마 처형검 ===
    public boolean hasExecutionSword;

    // === 하카리 잭팟 ===
    public boolean jackpotActive;
    public long jackpotEndTick;
    public long lastJackpotAttemptTick;

    // === 추가 상태 필드 ===
    public boolean infinityActive;
    public boolean curtainActive;
    public boolean overtimeWork;
    public boolean fallingBlossomActive;
    public long fallingBlossomUntil;
    public boolean simpleBarrierActive;
    public float shikigamiDmgBoost = 1.0f;

    // === 옷코츠 복사 술식 (§13-1 + §6-5) ===
    public String lastReceivedSkillId;        // 최근 피격된 스킬 ID (null = 없음)
    public int    lastReceivedBaseDamage;     // 해당 스킬의 baseDamage
    public int    lastReceivedCeCost;         // 해당 스킬의 ceCost
    public int    lastReceivedCooldownTicks;  // 해당 스킬의 cooldownTicks
    public boolean lastReceivedIsDomain;      // 영역 스킬 여부 (복사 불가)

    // === 천여주박 (CE 0 시 신체능력 버프) ===
    public boolean tenShadowsActive;

    // === 수동 방어 ===
    public boolean shieldActive;

    // === 영창 시스템 ===
    public boolean chanting;
    public long chantStartTick;

    // === 콤보 시스템 ===
    public int comboCount;
    public long comboLastHitTick;
    public UUID comboTargetUuid;

    // === XP / 등급 ===
    public int lastLoginDay; // epoch day (일일 접속 XP 중복 방지)
    public long lastDamageTakenTick; // Perfect 판정용

    // === 메구미 마허라가 의식 ===
    public int maharagaCounter; // 무하한 피격 횟수, 초기값 0

    // === 이타도리 흑섬 집중 ===
    public long blackFlashFocusEndTick; // 0 = 비활성, 양수 = 버프 만료 틱

    // === 흑섬 연속 발동 쿨다운 (구현 C-2) ===
    public long blackFlashCooldownUntil;

    // === 가이드북 수령 여부 (TASK-32) ===
    public boolean receivedGuideBook;

    // === 히구루마 재판 대상 UUID (TASK-34) ===
    public String trialTargetUuid;  // null = 없음

    // === 히구루마 단일-스킬 봉인 (Phase I-2) ===
    public Set<String> sealedSkills = new HashSet<>();
    public long sealExpireTick = 0L;
    public String lastUsedSkillId = "";
    public boolean evidenceAmplifyActive = false;

    // === 안티치트 (TASK J-2) ===
    public List<String> antiAbuseFlags = new ArrayList<>();
    public boolean quarantined = false;

    // === 주력석 경제 시스템 ===
    public long cursedStones;       // 주력석 보유량
    public int  masteryResetCount;  // 숙련도 초기화 횟수
    public long bounty;             // 현상금 누적액 (주령 진영)

    // === 주간 퀘스트 ===
    public long weeklyQuestDone = -1L; // 완료한 epoch week (-1 = 미완료)

    // === 의상 시스템 ===
    public String costumeId = "default";

    // === 쵸소 혈액 자원 ===
    public int bloodResource = 0;  // 0~5

    // === CE 조작 성장 ===
    public float ceControl = 1.0f;   // 상한 2.0, 숙련도 Lv.10 +0.10, 특급 달성 +0.20

    // === 퀘스트 진행 (P4-3) ===
    public Map<String, Integer> questProgress = new HashMap<>();
    public Set<String> completedDailyQuests   = new HashSet<>();
    public Set<String> completedWeeklyQuests  = new HashSet<>();
    public long lastQuestResetDay             = 0L;

    // === 시즌 XP (P4-7) ===
    public int seasonXp = 0;

    // === 캐릭터 재선택 이력 ===
    public int  characterResetCount          = 0;
    public long lastCharacterResetTimestamp  = 0L;

    // === 선택 책 지급 이력 (중복 지급 방지) ===
    public boolean hasReceivedSelectionBook  = false;

    // === 주령 포획 누적 (G-2) ===
    public int capturedSpiritCount           = 0;

    // === 속박 서약 양방향 패널티 (G-4) ===
    public String  pendingBindingVowSkillId  = null;
    public long    pendingBindingVowStartTick= 0L;
    public boolean vowSkillUsedThisVow       = false;

    // === 튜토리얼 완료 여부 (G-5-2) ===
    public boolean hasCompletedTutorial      = false;

    // === 비술사(천여주박) 신체능력 강화 ===
    public long  nsBurstExpireTick      = 0L;
    public float attackBoostMultiplier  = 1.0f;
    public float defenseBoostMultiplier = 1.0f;
    public long  nsShieldExpireTick     = 0L;
    public boolean nsDeathPreventUsed   = false;

    // === 스키마 버전 ===
    public int schemaVersion = 1;

    public static PlayerData createDefault(UUID uuid) {
        PlayerData d = new PlayerData();
        d.uuid                    = uuid;
        d.schemaVersion           = 1;
        d.bindingVowDeclaredTick  = -1L;
        d.grade                   = Grade.GRADE_4;
        d.trialState              = "IDLE";
        d.ceControl               = 1.0f;
        d.ceMax                   = 100.0f;
        d.ceCurrent               = 100.0f;
        d.hpMax                   = 20.0f;
        d.hpCurrent               = 20.0f;
        d.attackStat              = 10;
        d.defenseStat             = 10;
        d.speedStat               = 10;
        return d;
    }

    public PlayerData snapshot() {
        PlayerData copy = new PlayerData();
        copy.uuid                    = this.uuid;
        copy.characterId             = this.characterId;
        copy.grade                   = this.grade;
        copy.xp                      = this.xp;
        copy.mastery                 = this.mastery;
        copy.ceCurrent               = this.ceCurrent;
        copy.ceMax                   = this.ceMax;
        copy.hpCurrent               = this.hpCurrent;
        copy.hpMax                   = this.hpMax;
        copy.attackStat              = this.attackStat;
        copy.defenseStat             = this.defenseStat;
        copy.speedStat               = this.speedStat;
        copy.fingerCount             = this.fingerCount;
        copy.unlockedSkills          = new ArrayList<>(this.unlockedSkills);
        copy.cooldowns               = new HashMap<>(this.cooldowns);
        copy.bindingVowDeclaredTick  = this.bindingVowDeclaredTick;
        copy.domainCooldownUntil     = this.domainCooldownUntil;
        copy.jackpotCooldownUntil    = this.jackpotCooldownUntil;
        copy.curtainCooldownUntil    = this.curtainCooldownUntil;
        copy.trialState              = this.trialState;
        copy.burden                  = this.burden;
        copy.zoneActive              = this.zoneActive;
        copy.zoneEndTick             = this.zoneEndTick;
        copy.lastCombatTick          = this.lastCombatTick;
        copy.lastKnownIp             = this.lastKnownIp;
        copy.awakeningActive         = this.awakeningActive;
        copy.awakeningEndTick        = this.awakeningEndTick;
        copy.awakeningCooldownUntil  = this.awakeningCooldownUntil;
        copy.burstActive             = this.burstActive;
        copy.burstEndTick            = this.burstEndTick;
        copy.deadShikigamiIds        = new ArrayList<>(this.deadShikigamiIds);
        copy.healingActive           = this.healingActive;
        copy.zonePenaltyUntilTick    = this.zonePenaltyUntilTick;
        copy.zoneEntryTick           = this.zoneEntryTick;
        copy.lastAttackTick          = this.lastAttackTick;
        copy.hasExecutionSword       = this.hasExecutionSword;
        copy.jackpotActive           = this.jackpotActive;
        copy.jackpotEndTick          = this.jackpotEndTick;
        copy.lastJackpotAttemptTick  = this.lastJackpotAttemptTick;
        copy.infinityActive          = this.infinityActive;
        copy.curtainActive           = this.curtainActive;
        copy.overtimeWork            = this.overtimeWork;
        copy.fallingBlossomActive    = this.fallingBlossomActive;
        copy.fallingBlossomUntil     = this.fallingBlossomUntil;
        copy.simpleBarrierActive     = this.simpleBarrierActive;
        copy.shikigamiDmgBoost           = this.shikigamiDmgBoost;
        copy.lastReceivedSkillId         = this.lastReceivedSkillId;
        copy.lastReceivedBaseDamage      = this.lastReceivedBaseDamage;
        copy.lastReceivedCeCost          = this.lastReceivedCeCost;
        copy.lastReceivedCooldownTicks   = this.lastReceivedCooldownTicks;
        copy.lastReceivedIsDomain        = this.lastReceivedIsDomain;
        copy.tenShadowsActive            = this.tenShadowsActive;
        copy.shieldActive                = this.shieldActive;
        copy.chanting                    = this.chanting;
        copy.chantStartTick              = this.chantStartTick;
        copy.comboCount                  = this.comboCount;
        copy.comboLastHitTick            = this.comboLastHitTick;
        copy.comboTargetUuid             = this.comboTargetUuid;
        copy.lastLoginDay                = this.lastLoginDay;
        copy.lastDamageTakenTick         = this.lastDamageTakenTick;
        copy.maharagaCounter             = this.maharagaCounter;
        copy.blackFlashFocusEndTick      = this.blackFlashFocusEndTick;
        copy.blackFlashCooldownUntil     = this.blackFlashCooldownUntil;
        copy.receivedGuideBook           = this.receivedGuideBook;
        copy.trialTargetUuid             = this.trialTargetUuid;
        copy.sealedSkills                = new HashSet<>(this.sealedSkills);
        copy.sealExpireTick              = this.sealExpireTick;
        copy.lastUsedSkillId             = this.lastUsedSkillId;
        copy.evidenceAmplifyActive       = this.evidenceAmplifyActive;
        copy.antiAbuseFlags              = new ArrayList<>(this.antiAbuseFlags);
        copy.quarantined                 = this.quarantined;
        copy.cursedStones                = this.cursedStones;
        copy.masteryResetCount           = this.masteryResetCount;
        copy.bounty                      = this.bounty;
        copy.weeklyQuestDone             = this.weeklyQuestDone;
        copy.costumeId                   = this.costumeId;
        copy.bloodResource               = this.bloodResource;
        copy.ceControl                   = this.ceControl;
        copy.questProgress               = new HashMap<>(this.questProgress);
        copy.completedDailyQuests        = new HashSet<>(this.completedDailyQuests);
        copy.completedWeeklyQuests       = new HashSet<>(this.completedWeeklyQuests);
        copy.lastQuestResetDay           = this.lastQuestResetDay;
        copy.seasonXp                    = this.seasonXp;
        copy.characterResetCount         = this.characterResetCount;
        copy.lastCharacterResetTimestamp = this.lastCharacterResetTimestamp;
        copy.hasReceivedSelectionBook    = this.hasReceivedSelectionBook;
        copy.capturedSpiritCount         = this.capturedSpiritCount;
        copy.pendingBindingVowSkillId    = this.pendingBindingVowSkillId;
        copy.pendingBindingVowStartTick  = this.pendingBindingVowStartTick;
        copy.vowSkillUsedThisVow         = this.vowSkillUsedThisVow;
        copy.hasCompletedTutorial        = this.hasCompletedTutorial;
        copy.nsBurstExpireTick           = this.nsBurstExpireTick;
        copy.attackBoostMultiplier       = this.attackBoostMultiplier;
        copy.defenseBoostMultiplier      = this.defenseBoostMultiplier;
        copy.nsShieldExpireTick          = this.nsShieldExpireTick;
        copy.nsDeathPreventUsed          = this.nsDeathPreventUsed;
        copy.schemaVersion               = this.schemaVersion;
        return copy;
    }
}

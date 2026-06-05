package com.jjk.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerData {

    // === 기본 정보 ===
    public UUID uuid;
    public String characterId;
    public String grade;
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

    // === 가이드북 수령 여부 (TASK-32) ===
    public boolean receivedGuideBook;

    // === 히구루마 재판 대상 UUID (TASK-34) ===
    public String trialTargetUuid;  // null = 없음

    // === 주력석 경제 시스템 ===
    public long cursedStones;       // 주력석 보유량
    public int  masteryResetCount;  // 숙련도 초기화 횟수
    public long bounty;             // 현상금 누적액 (주령 진영)

    // === 주간 퀘스트 ===
    public long weeklyQuestDone = -1L; // 완료한 epoch week (-1 = 미완료)

    // === 의상 시스템 ===
    public String costumeId = "default";

    // === 스키마 버전 ===
    public int schemaVersion = 1;

    public static PlayerData createDefault(UUID uuid) {
        PlayerData d = new PlayerData();
        d.uuid                    = uuid;
        d.schemaVersion           = 1;
        d.bindingVowDeclaredTick  = -1L;
        d.grade                   = "4급";
        d.trialState              = "IDLE";
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
        copy.receivedGuideBook           = this.receivedGuideBook;
        copy.trialTargetUuid             = this.trialTargetUuid;
        copy.cursedStones                = this.cursedStones;
        copy.masteryResetCount           = this.masteryResetCount;
        copy.bounty                      = this.bounty;
        copy.weeklyQuestDone             = this.weeklyQuestDone;
        copy.costumeId                   = this.costumeId;
        copy.schemaVersion               = this.schemaVersion;
        return copy;
    }
}
